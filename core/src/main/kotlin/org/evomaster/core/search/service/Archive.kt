package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.FeedbackDirectedSampling.FOCUSED_QUICKEST
import org.evomaster.core.EMConfig.FeedbackDirectedSampling.LAST
import org.evomaster.core.Lazy
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual
import org.evomaster.core.search.service.monitor.SearchProcessMonitor
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.tracer.ArchiveMutationTrackService

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


class Archive<T> where T : Individual {

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var dpc: AdaptiveParameterControl

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var processMonitor: SearchProcessMonitor

    @Inject
    private lateinit var tracker : ArchiveMutationTrackService
    /**
     * Key -> id of the target
     *
     * Value -> sorted list of best individuals for that target
     */
    private val populations = mutableMapOf<Int, MutableList<EvaluatedIndividual<T>>>()

    /**
     * Key -> id of the target
     *
     * Value -> how often we sampled from the buffer for that target since
     *          last fitness improvement.
     *          Note: such counter will be reset when a fitness improvement
     *          is obtained for that target is obtained.
     *          This means that an infeasible / hard target will not get
     *          its counter reset once the final local optima is reached
     */
    private val samplingCounter = mutableMapOf<Int, Int>()


    /**
     * Key -> id of the target
     *
     * Value -> keep track of how long (in number of sampled individuals)
     *          it took last time there was an improvement for this target.
     */
    private val lastImprovement = mutableMapOf<Int, Int>()


    /**
     * Key -> id of the target
     *
     * Value -> latest evaluated individual there was an improvement for this target.
     */
    //private val latestImprovement = mutableMapOf<Int, Int>()

    /**
     * Id of last target used for sampling
     */
    private var lastChosen: Int? = null


    fun extractSolution(): Solution<T> {

        val uniques = getUniquePopulation()

        return Solution(uniques.toMutableList(), config.testSuiteFileName, Termination.NONE)
    }

    fun extractPartialSolution(): Solution<T> {

        val uniques = getUniquePopulation()

        return Solution(uniques.toMutableList(), config.testSuiteFileName, Termination.IN_PROGRESS)
    }

    private fun getUniquePopulation(): MutableSet<EvaluatedIndividual<T>> {
        /*
            Note: no equals() is defined, so Set is based
            on refs to the heap.
            This is not an issue, as each individual is copied
            when sampled.
            Here, as an individual can go to many populations,
            we want to avoiding it counting it several times.
         */
        val uniques = mutableSetOf<EvaluatedIndividual<T>>()

        populations.entries.forEach { e ->
            if (isCovered(e.key)) {
                val ind = e.value[0]
                uniques.add(ind)
            }
        }
        return uniques
    }

    fun isEmpty() = populations.isEmpty()

    /**
     * Get a copy of an individual in the archive.
     * Different kinds of heuristics are used to choose
     * the best "candidate" most useful for the search
     */
    fun sampleIndividual(): EvaluatedIndividual<T> {

        if (isEmpty()) {
            throw IllegalStateException("Empty archive")
        }

        var toChooseFrom = notCoveredTargets()
        if (toChooseFrom.isEmpty()) {
            //this means all current targets are covered
            toChooseFrom = populations.keys.toSet()
        }


        val chosenTarget = chooseTarget(toChooseFrom)
        lastChosen = chosenTarget

        val candidates = populations[chosenTarget] ?:
                //should never happen, unless of bug
                throw IllegalStateException("Target $chosenTarget has no candidate individual")


        incrementCounter(chosenTarget)

        sortAndShrinkIfNeeded(candidates, chosenTarget)

        val notTimedout = candidates.filter {
            !it.results.any { res -> res is RestCallResult && res.getTimedout() }
        }

        /*
            If possible avoid sampling tests that did timeout
         */
        val chosen = if (!notTimedout.isEmpty()) {
            randomness.choose(notTimedout)
        } else {
            randomness.choose(candidates)
        }

        return chosen.copy(tracker.getCopyFilterForEvalInd(chosen))
    }

    private fun chooseTarget(toChooseFrom: Set<Int>): Int {

        return when (config.feedbackDirectedSampling) {
            LAST -> toChooseFrom.minBy {
                val counter = samplingCounter.getOrDefault(it, 0)
                val p = populations[it]!!
                val time = p[p.lastIndex].executionTimeMs //time of best individual
                if(!config.useTimeInFeedbackSampling || time == Long.MAX_VALUE){
                     counter.toLong()
                } else {
                    /*
                    WARNING: this does introduce some form of non-determinism,
                    as timestamps can vary.
                 */
                    (counter * time)
                }
            }!!
            FOCUSED_QUICKEST ->
                handleFocusedQuickest(toChooseFrom)
            else ->
                randomness.choose(toChooseFrom)
        }
    }

    private fun handleFocusedQuickest(toChooseFrom: Set<Int>): Int {

        val lc = lastChosen

        if (lc != null
                && toChooseFrom.contains(lc)
                /*
                    the X can happen if there was never an improvement.
                    so we still want to try 2X times before going to another
                    one
                 */
                && (samplingCounter[lc] ?: 0) < (lastImprovement[lc] ?: 10) * 2
                ) {
            return lc
        }

        /*
        We can't reuse the previous target. Need to pick up
        a new one
        */

        val index = toChooseFrom
                .filter {
                    val previous = lastImprovement[it]
                    previous != null &&
                            samplingCounter[it]!! < previous * 2
                }
                .minBy { lastImprovement[it]!! }

        return index ?: toChooseFrom.minBy {
            samplingCounter.getOrDefault(it, 0)
        }!!
    }

    /**
     * update counter by 1
     */
    private fun incrementCounter(target: Int) {
        samplingCounter.putIfAbsent(target, 0)
        val counter = samplingCounter[target]!!
        samplingCounter.put(target, counter + 1)
    }

    private fun reportImprovement(target: Int) {

        val counter = samplingCounter.getOrDefault(target, 0)
        lastImprovement.put(target, counter)
        samplingCounter.put(target, 0)

        //latestImprovement[target] = time.evaluatedIndividuals
    }

    /**
     * Useful for debugging
     */
    fun encounteredTargetDescriptions(): List<String> {

        return populations.entries
                .map { e -> "key ${e.key}: ${idMapper.getDescriptiveId(e.key)} , size=${e.value.size}" }
                .sorted()
    }

    /**
     * Useful for debugging
     */
    fun reachedTargetHeuristics(): List<String> {

        return populations.entries
                .map { e -> "key ${e.key} -> best heuristics=${e.value.map { it.fitness.computeFitnessScore() }.max()}" }
                .sorted()
    }

    fun numberOfCoveredTargets(): Int {
        return populations.keys.stream().filter { isCovered(it) }.count().toInt()
    }

    fun numberOfReachedButNotCoveredTargets(): Int {
        return populations.keys.stream().filter { ! isCovered(it) }.count().toInt()
    }

    fun numberOfReachedTargets() : Int = populations.size

    fun averageTestSizeForReachedButNotCovered() : Double {
        return populations.entries
                .filter { ! isCovered(it.key) }
                .flatMap { it.value }
                .map { it.individual.size() }
                .average()
    }

    /**
     * Get all known targets that are not fully covered
     *
     * @return a list of ids
     */
    fun notCoveredTargets(): Set<Int> {

        /*
            FIXME: performance, use cache for non-covered.
            As we can have 10s of thousands of covered targets,
            iterating over them is expensive
         */

        return populations.keys.filter { !isCovered(it) }.toSet()
    }


    fun wouldReachNewTarget(ei: EvaluatedIndividual<T>): Boolean {

        return ei.fitness.getViewOfData()
                .filter { it.value.distance > 0.0 }
                .map { it.key }
                .any { populations[it]?.isEmpty() ?: true }
    }

    fun identifyNewTargets(ei: EvaluatedIndividual<T>, targetInfo: MutableMap<Int, EvaluatedMutation>) {

        ei.fitness.getViewOfData()
                .filter { it.value.distance > 0.0 && populations[it.key]?.isEmpty() ?: true}
                .forEach { t->
                    targetInfo[t.key] = EvaluatedMutation.NEWLY_IDENTIFIED
                }
    }

    /**
     * @return true if the new individual was added to the archive
     */
    fun addIfNeeded(ei: EvaluatedIndividual<T>): Boolean {

        val copy = ei.copy(tracker.getCopyFilterForEvalInd(ei))
        var added = false
        var anyBetter = false

        for ((k, v) in ei.fitness.getViewOfData()) {

            if (v.distance == 0.0) {
                /*
                    No point adding an individual with no impact
                    on a given target
                 */
                continue
            }

            val current = populations.getOrPut(k, { mutableListOf() })

            //ind does reach a new target?
            if (current.isEmpty()) {
                current.add(copy)
                added = true
                time.newActionImprovement()
                reportImprovement(k)

                if (isCovered(k)) {
                    time.newCoveredTarget()
                }

                continue
            }

            val maxed = FitnessValue.isMaxValue(v.distance)

            if (isCovered(k) && maxed) {
                /*
                    Target is already covered. But could it
                    be that new individual covers it as well,
                    and it is better?

                    Recall: during the search, the fitness score could be
                    partial, so this check on collateral coverage likely
                    will not be so effective
                 */
                Lazy.assert{current.size == 1} //if covered, should keep only one solution in buffer

                val shorter = copy.individual.size() < current[0].individual.size()
                val sameLengthButBetterScore = (copy.individual.size() == current[0].individual.size())
                        && (copy.fitness.computeFitnessScore() > current[0].fitness.computeFitnessScore())

                /*
                 * Once a target is covered, we check if can cover it with a new test that is shorter.
                 * Given two tests covering the same target, both with same length, then we prefer
                 * the one that has most collateral coverage
                 */
                if (shorter || sameLengthButBetterScore) {
                    current[0] = copy
                    added = true
                    time.newActionImprovement()
                    reportImprovement(k)
                }
                continue
            }

            if (maxed) {
                current.clear() //remove all existing non-optimal solutions
                current.add(copy)
                added = true
                time.newActionImprovement()
                reportImprovement(k)
                time.newCoveredTarget()
                continue
            }


            //handle regular case.
            sortAndShrinkIfNeeded(current, k)

            /*
                as the population are internally sorted by fitness, the individual
                at position [0] would be the worst
             */

            val curr = current[0]
            Lazy.assert {
                curr.fitness.size == curr.individual.size().toDouble()
                        &&
                copy.fitness.size == copy.individual.size().toDouble()
            }

            /*
              config.minimumSizeControl = 2 is to
                avoid reducing tests to size 1 if extra was better.
                With at least 2 actions, we can have a WRITE followed by a READ
            */
            val better = copy.fitness.betterThan(k, curr.fitness, config.secondaryObjectiveStrategy, config.bloatControlForSecondaryObjective, config.minimumSizeControl)
            anyBetter = anyBetter || better

            if (better) {
                time.newActionImprovement()
                reportImprovement(k)
            }

            val limit = dpc.getArchiveTargetLimit()

            /*
             individual can be added only if the target k is not covered.
             If a target is covered and a 'better'(e.g., shorter) individual appears,
             it would be handled as replacement.
             */
            if (!isCovered(k) && current.size < limit) {
                //we have space in the buffer, regardless of fitness
                current.add(copy)
                added = true

                continue
            }

            val equivalent = copy.fitness.equivalent(k, curr.fitness, config.secondaryObjectiveStrategy)

            if (better || equivalent) {
                /*
                    replace worst element, if copy is not worse than it (but not necessarily better).
                    However this is base on heuristics values and size, but NOT execution time

                    TODO would it makes sense to do something like subsumes() where
                    execution time is taken into account?
                 */
                current[0] = copy
                added = true
            }
        }
        processMonitor.record(added, anyBetter, ei)

        ei.hasImprovement = anyBetter
        return added
    }

    /*
       Ascending sort based on heuristics and, if same value, on negation of size.
       Worst element will be the first, best the last.
       Resize the list if needed
     */
    private fun sortAndShrinkIfNeeded(list: MutableList<EvaluatedIndividual<T>>, target: Int) {

        /*
            First look at heuristics for the target.
            That is most important value.
            In case of same, then look at the extra heuristics.
            If all the same, then do prefer shorter tests.
         */

        list.sortWith(compareBy<EvaluatedIndividual<T>>
        { it.fitness.getHeuristic(target) }
                .thenComparator { a, b -> a.fitness.compareExtraToMinimize(target, b.fitness, config.secondaryObjectiveStrategy) }
                .thenBy { -it.individual.size() }
                .thenBy{ if(config.useTimeInFeedbackSampling) -it.executionTimeMs else 0L})

        val limit = dpc.getArchiveTargetLimit()
        while (list.size > limit) {
            //remove worst, ie the one with lowest heuristic value
            list.removeAt(0)
        }
    }

    fun isCovered(target: Int): Boolean {

        val current = populations[target] ?: return false

        if (current.size != 1) {
            return false
        }

        return current[0].fitness.doesCover(target)
    }

    /**
     * useful for debugging
     */
    fun getReachedTargetHeuristics(target: Int) : Double?{
        return populations[target]?.map { v-> v.fitness.getHeuristic(target) }?.max()
    }

    /**
     * @return current population
     */
    fun getSnapshotOfBestIndividuals(): Map<Int, MutableList<EvaluatedIndividual<T>>>{
        return populations
    }

    /**
     * @return current samplingCounter
     */
    fun getSnapshotOfSamplingCounter() : Map<Int, Int>{
        return samplingCounter
    }

    /**
     * @return a list of pairs which is composed of target id (first) and corresponding tests (second)
     */
    fun exportCoveredTargetsAsPair(solution: Solution<*>) : List<Pair<String, List<Int>>>{

        return populations.keys
                .asSequence()
                .filter { isCovered(it) }
                .map { t->
                    Pair(idMapper.getDescriptiveId(t), solution.individuals.mapIndexed { index, f-> if (f.fitness.doesCover(t)) index else -1 }.filter { it != -1 })
                }.toList()
    }

    /**
     * @return an existing ImpactsOfIndividual which includes same action with [other]
     */
    fun findImpactInfo(other: Individual) : ImpactsOfIndividual?{
        return populations.values.find {
            it.any { i-> i.individual.sameActions(other) }
        }.run {
            if (this == null || this.isEmpty())
                null
            else
                find{i -> i.individual.sameActions(other)}!!.impactInfo?.clone()
        }
    }


    fun saveSnapshot(){
        if (!config.saveArchiveAfterMutation) return

        val index = time.evaluatedIndividuals
        val archiveContent = notCoveredTargets().filter { it >= 0 }.map { "$index,${it to getReachedTargetHeuristics(it)},${idMapper.getDescriptiveId(it)}" }

        val apath = Paths.get(config.archiveAfterMutationFile)
        if (apath.parent != null) Files.createDirectories(apath.parent)
        if (Files.notExists(apath)) Files.createFile(apath)

        if (archiveContent.isNotEmpty()) Files.write(apath, archiveContent, StandardOpenOption.APPEND)
    }

//    fun chooseLatestImprovedTargets(size : Int) : Set<Int>{
//        return latestImprovement.asSequence().sortedByDescending { it.value }.toList().subList(0, min(size, latestImprovement.size)).map { it.key }.toSet()
//    }
//
//    fun chooseImproveTargetsAfter(index : Int) : Set<Int> = latestImprovement.filterValues { it >= index }.keys

}
