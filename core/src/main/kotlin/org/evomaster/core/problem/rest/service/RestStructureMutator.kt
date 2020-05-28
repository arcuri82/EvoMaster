package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestStructureMutator : StructureMutator() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestStructureMutator::class.java)
    }

    @Inject
    private lateinit var archiveMutator: ArchiveMutator

    @Inject
    private lateinit var sampler: RestSampler

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {

        if (config.shouldGenerateSqlData()) {
            generateSqlData(individual, mutatedGenes)
        }

        if (config.shouldGenerateMongoData()) {
            generateMongoData(individual, mutatedGenes)
        }
    }

    private fun generateMongoData(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        val ind = individual.individual as? RestIndividual
                ?: throw IllegalArgumentException("Invalid individual type")

        // TODO create
    }

    private fun generateSqlData(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        val ind = individual.individual as? RestIndividual
                ?: throw IllegalArgumentException("Invalid individual type")

        val fw = individual.fitness.getViewOfAggregatedFailedWhere()
                //TODO likely to remove/change once we ll support VIEWs
                .filter { sampler.canInsertInto(it.key) }

        if (fw.isEmpty()) {
            return
        }

        if (ind.dbInitialization.isEmpty()
                || !ind.dbInitialization.any { it.representExistingData }) {
            //add existing data only once
            ind.dbInitialization.addAll(0, sampler.existingSqlData)
            mutatedGenes?.addedInitializationGenes?.addAll(sampler.existingSqlData.flatMap { it.seeGenes() })
        }

        val max = config.maxSqlInitActionsPerMissingData

        var missing = findMissing(fw, ind)

        while (!missing.isEmpty()) {

            val first = missing.entries.first()

            val k = randomness.nextInt(1, max)

            (0 until k).forEach {
                val insertions = sampler.sampleSqlInsertion(first.key, first.value)
                /*
                    New action should be before existing one, but still after the
                    initializing ones
                 */
                val position = sampler.existingSqlData.size
                ind.dbInitialization.addAll(position, insertions)
                mutatedGenes?.addedInitializationGenes?.addAll(insertions.flatMap { it.seeGenes() })
            }

            /*
                When we miss A and B, and we add for A, it can still happen that
                then B is covered as well. For example, if A has a non-null
                foreign key to B, then generating an action for A would also
                imply generating an action for B as well.
                So, we need to recompute "missing" each time
             */
            missing = findMissing(fw, ind)
        }

        if (config.generateSqlDataWithDSE) {
            //TODO DSE could be plugged in here
        }

        ind.repairInitializationActions(randomness)
    }

    private fun findMissing(fw: Map<String, Set<String>>, ind: RestIndividual): Map<String, Set<String>> {

        return fw.filter { e ->
            //shouldn't have already an action adding such SQL data
            ind.dbInitialization.none { a ->
                a.table.name.equals(e.key, ignoreCase = true) && e.value.all { c ->
                    // either the selected column is already in existing action
                    (c != "*" && a.selectedColumns.any { x ->
                        x.name.equals(c, ignoreCase = true)
                    }) // or we want all, and existing action has all columns
                            || (c == "*" && a.table.columns.map { it.name.toLowerCase() }
                            .containsAll(a.selectedColumns.map { it.name.toLowerCase() }))
                }
            }
        }
    }


    override fun mutateStructure(individual: Individual, mutatedGenes: MutatedGeneSpecification?) {
        if (individual !is RestIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        /*
            TODO should be enable the adding/removing of SQL commands here?
            Or should that be better to handle with DSE?
         */


        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        when (individual.sampleType) {
            SampleType.RANDOM -> mutateForRandomType(individual, mutatedGenes)

            SampleType.SMART_GET_COLLECTION -> mutateForSmartGetCollection(individual, mutatedGenes)

            SampleType.SMART -> throw IllegalStateException(
                    "SMART sampled individuals shouldn't be marked for structure mutations")

            //this would be a bug
            else -> throw IllegalStateException("Cannot handle sample type ${individual.sampleType}")
        }
    }

    private fun mutateForSmartGetCollection(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?) {
        /*
            recall: in this case, we have 1 or more POST on same
            collection, followed by a single GET

            However, in case of path parameters (eg "/x/{id}/collection")
            before the collection endpoint, there might be one or more POSTs
            to setup the intermediary resources
         */

        (0 until ind.seeActions().size - 1).forEach {
            val a = ind.seeActions()[it]
            Lazy.assert { a !is RestCallAction || a.verb == HttpVerb.POST }
        }
        Lazy.assert { val a = ind.seeActions().last(); a is RestCallAction && a.verb == HttpVerb.GET }

        val indices = ind.seeActions().indices
                .filter { i ->
                    val a = ind.seeActions()[i]
                    /*
                        one simple way to distinguish the POST on collection is that
                        they are not chaining a location, as GET is on same endpoint
                    */
                    a is RestCallAction && !a.saveLocation && a.verb == HttpVerb.POST
                }

        if (indices.isEmpty()) {
            /*
                Nothing we can do here. Cannot delete a POST, and
                neither add a new one, as we have no template for
                it in the test to duplicate.
             */
            return
        }

        if (indices.size > 1 &&
                (randomness.nextBoolean() ||
                        ind.seeActions().size == config.maxTestSize)) {

            //delete one POST, but NOT the GET
            val chosen = randomness.choose(indices)

            //save mutated genes
            val removedActions = ind.getResourceCalls()[chosen].actions
            assert(removedActions.size == 1)
            mutatedGenes?.removedGene?.addAll(removedActions.first().seeGenes())
            mutatedGenes?.mutatedPosition?.add(chosen)

            //ind.seeActions().removeAt(chosen)
            ind.removeResourceCall(chosen)

        } else {
            //insert a new POST on the collection
            val idx = indices.last()

            val postTemplate = ind.seeActions()[idx] as RestCallAction
            Lazy.assert { postTemplate.verb == HttpVerb.POST && !postTemplate.saveLocation }

            val post = sampler.createActionFor(postTemplate, ind.seeActions().last() as RestCallAction)

            //save mutated genes
            mutatedGenes?.addedGenes?.addAll(post.seeGenes())
            mutatedGenes?.mutatedPosition?.add(idx)

            /*
                where it is inserted should not matter, as long as
                it is before the last GET, but after all the other initializing
                POSTs
             */
            //ind.seeActions().add(idx, post)
            ind.addResourceCall(idx, RestResourceCalls(actions = mutableListOf(post)))
        }
    }

    private fun mutateForRandomType(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?) {

        if (ind.seeActions().size == 1) {
            val sampledAction = sampler.sampleRandomAction(0.05)

            //save mutated genes
            mutatedGenes?.addedGenes?.addAll(sampledAction.seeGenes())
            mutatedGenes?.mutatedPosition?.add(ind.seeActions().size)

            //ind.seeActions().add(sampledAction)
            ind.addResourceCall(RestResourceCalls(actions = mutableListOf(sampledAction)))

            //if (config.enableCompleteObjects && (sampledAction is RestCallAction)) sampler.addObjectsForAction(sampledAction, ind)
            return
        }

        if (randomness.nextBoolean() || ind.seeActions().size == config.maxTestSize) {

            //delete one at random
            log.trace("Deleting action from test")
            val chosen = randomness.nextInt(ind.seeActions().size)

            //save mutated genes
            val removedActions = ind.getResourceCalls()[chosen].actions
            assert(removedActions.size == 1)
            mutatedGenes?.removedGene?.addAll(removedActions.first().seeGenes())
            mutatedGenes?.mutatedPosition?.add(chosen)

            //ind.seeActions().removeAt(chosen)
            ind.removeResourceCall(chosen)

        } else {

            //add one at random
            log.trace("Adding action to test")
            val sampledAction = sampler.sampleRandomAction(0.05)
            val chosen = randomness.nextInt(ind.seeActions().size)
            //ind.seeActions().add(chosen, sampledAction)
            ind.addResourceCall(chosen, RestResourceCalls(actions = mutableListOf(sampledAction)))

            //save mutated genes
            mutatedGenes?.addedGenes?.addAll(sampledAction.seeGenes())
            mutatedGenes?.mutatedPosition?.add(chosen)

            //if (config.enableCompleteObjects && (sampledAction is RestCallAction)) sampler.addObjectsForAction(sampledAction, ind)
            // BMR: Perhaps we could have a function for individual.addAction(action) which would cover both
            // adding the action and the associated objects and help encapsulate the individual more?
        }

    }

}