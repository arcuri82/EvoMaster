package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 */
abstract class Gene(var name: String, children: List<out StructuralElement>) : StructuralElement(children){

    companion object{
        private val log: Logger = LoggerFactory.getLogger(Gene::class.java)
    }

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    /**
     * Make a copy of this gene.
     */
    final override fun copy() : Gene{
        val copy = super.copy()
        if (copy !is Gene)
            throw IllegalStateException("mismatched type: the type should be Gene, but it is ${this::class.java.simpleName}")
        return copy
    }

    override fun copyContent(): Gene {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }

    /**
     * weight for mutation
     * For example, higher the weight, the higher the chances to be selected for mutation
     */
    open fun mutationWeight() : Double = 1.0

    /**
     * Specify if this gene can be mutated during the search.
     * Typically, it will be true, apart from some special cases.
     */
    open fun isMutable() = true

    /**
     * Specify if this gene should be printed in the output test.
     * In other words, if this genotype directly influences the
     * phenotype
     */
    open fun isPrintable() = true


    /**
     *   Randomize the content of this gene.
     *
     *   @param randomness the source of non-determinism
     *   @param forceNewValue whether we should force the change of value. When we do mutation,
     *          it could otherwise happen that a value is replace with itself
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     */
    abstract fun randomize(
            randomness: Randomness,
            forceNewValue: Boolean,
            allGenes: List<Gene> = listOf())

    /**
     * A mutation is just a small change.
     * Apply a mutation to the current gene.
     * Regarding the gene,
     * 1) there might exist multiple internal genes i.e.,[candidatesInternalGenes].
     *  In this case, we first apply [selectSubset] to select a subset of internal genes.
     *  then apply mutation on each of the selected genes.
     * 2) When there is no need to do further selection, we apply [mutate] on the current gene.
     *
     *   @param randomness the source of non-determinism
     *   @param apc parameter control
     *   @param mwc mutation weight control
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     *   @param interalGeneSelectionStrategy a strategy to select internal genes to mutate
     *   @param enableAdaptiveMutation whether apply adaptive gene mutation, e.g., archive-based gene mutation
     *   @param additionalGeneMutationInfo contains additional info for gene mutation
     */
    fun standardMutation(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            mwc: MutationWeightControl,
            allGenes: List<Gene> = listOf(),
            internalGeneSelectionStrategy: SubsetGeneSelectionStrategy = SubsetGeneSelectionStrategy.DEFAULT,
            enableAdaptiveGeneMutation: Boolean = false,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo? = null
    ){
        //if impact is not able to obtain, adaptive-gene-mutation should also be disabled
        val internalGenes = candidatesInternalGenes(randomness, apc, allGenes, internalGeneSelectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
        if (internalGenes.isEmpty()){
            val mutated = mutate(randomness, apc, mwc, allGenes, internalGeneSelectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)
            if (!mutated) throw IllegalStateException("leaf mutation is not implemented")
        }else{
            val selected = selectSubset(internalGenes, randomness, apc, mwc, allGenes, internalGeneSelectionStrategy, enableAdaptiveGeneMutation, additionalGeneMutationInfo)

            selected.forEach{
                var mutateCounter = 0
                do {
                    it.first.standardMutation(randomness, apc, mwc, allGenes, internalGeneSelectionStrategy, enableAdaptiveGeneMutation, it.second)
                    mutateCounter +=1
                }while (!mutationCheck() && mutateCounter <=3)
                if (!mutationCheck()){
                    if (log.isTraceEnabled)
                        log.trace("invoke GeneUtils.repairGenes")
                    GeneUtils.repairGenes(listOf(this))
                }
            }
        }

        //sync binding gene after value mutation
        syncBindingGenesBasedOnThis()
    }

    /**
     * mutated gene should pass the check if needed, eg, DateGene
     *
     * In some cases, we must have genes with 'valid' values.
     * For example, a date with month 42 would be invalid.
     * On the one hand, it can still be useful for robustness testing
     * to provide such invalid values in a HTTP call. On the other hand,
     * it would be pointless to try to add it directly into a database,
     * as that SQL command would simply fail without any SUT code involved.
     */
    open fun mutationCheck() : Boolean = true

    /**
     * @return whether to apply a subset selection for internal genes to mutate
     */
    open fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?)
            = listOf<Gene>()

    /**
     * @return a subset of internal genes to apply mutations
     */
    open fun selectSubset(internalGenes: List<Gene>,
                          randomness: Randomness,
                          apc: AdaptiveParameterControl,
                          mwc: MutationWeightControl,
                          allGenes: List<Gene> = listOf(),
                          selectionStrategy: SubsetGeneSelectionStrategy,
                          enableAdaptiveGeneMutation: Boolean,
                          additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        return  when(selectionStrategy){
            SubsetGeneSelectionStrategy.DEFAULT -> {
                val s = randomness.choose(internalGenes)
                listOf( s to additionalGeneMutationInfo?.copyFoInnerGene( null,s))
            }
            SubsetGeneSelectionStrategy.DETERMINISTIC_WEIGHT ->
                mwc.selectSubGene(candidateGenesToMutate = internalGenes, adaptiveWeight = false).map { it to additionalGeneMutationInfo?.copyFoInnerGene(null, it) }
            SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT -> {
                additionalGeneMutationInfo?: throw IllegalArgumentException("additionalGeneSelectionInfo should not be null")
                if (additionalGeneMutationInfo.impact == null)
                    mwc.selectSubGene(candidateGenesToMutate = internalGenes, adaptiveWeight = false).map { it to additionalGeneMutationInfo.copyFoInnerGene(null, it) }
                else
                    adaptiveSelectSubset(randomness, internalGenes, mwc, additionalGeneMutationInfo)
            }
        }.also {
            if (it.isEmpty())
                throw IllegalStateException("with $selectionStrategy strategy and ${internalGenes.size} candidates, none is selected to mutate")
            if (it.any { a -> a.second?.impact?.validate(a.first) == false})
                throw IllegalStateException("mismatched impact for gene ${it.filter { a -> a.second?.impact?.validate(a.first) == false}.map { "${it.first}:${it.second}" }.joinToString(",")}")
        }
    }

    open fun adaptiveSelectSubset(randomness: Randomness,
                                  internalGenes: List<Gene>,
                                  mwc: MutationWeightControl,
                                  additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        throw IllegalStateException("adaptive gene selection is unavailable for the gene")
    }

    /**
     * mutate the current gene if there is no need to apply selection, i.e., when [candidatesInternalGenes] is empty
     */
    open fun mutate(randomness: Randomness,
                    apc: AdaptiveParameterControl,
                    mwc: MutationWeightControl,
                    allGenes: List<Gene> = listOf(),
                    selectionStrategy: SubsetGeneSelectionStrategy,
                    enableAdaptiveGeneMutation: Boolean,
                    additionalGeneMutationInfo: AdditionalGeneMutationInfo?) = false

    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     *
     * "foo" -> "\"foo\"" -> printed as "foo"
     *
     * @param previousGenes previous genes which are necessary to look at
     * to determine the actual value of this gene
     * @param mode some genes could be printed in different ways, like an
     * object printed as JSON or XML
     * @param targetFormat different target formats may have different rules
     * regarding what characters need to be escaped (e.g. the $ char in Kotlin)
     * If the [targetFormat] is set to null, no characters are escaped.
     */
    abstract fun getValueAsPrintableString(
            previousGenes: List<Gene> = listOf(),
            mode: GeneUtils.EscapeMode? = null,
            targetFormat: OutputFormat? = null,
            /**
             * Generic boolean, used for extra info, if needed.
             *
             * This was introduced mainly to deal with the printing of objects in GraphQL.
             * Specify if the name of object should be printed or not, or just directly the
             * object {} definition, ie,
             * foo {...}
             * vs
             * {...}
             */
            extraCheck: Boolean = false
    ) : String


    open fun getValueAsRawString() = getValueAsPrintableString(targetFormat = null)
    /*
    Note: above, null target format means that no characters are escaped.
     */

    abstract fun copyValueFrom(other: Gene)

    /**
     * If this gene represents a variable, then return its name.
     */
    open fun getVariableName() = name

    /**
     * Genes might have other genes inside (eg, think of array).
     * @param excludePredicate is used to configure which genes you do not want to show genes inside.
     *      For instance, an excludePredicate is {gene : Gene -> (gene is TimeGene)}, then when flatView of a Gene including TimeGene,
     *      the genes inside e.g., hour: IntegerGene will be not viewed, but TimeGene will be viewed.
     * @return a recursive list of all nested genes, "this" included
     */
    open fun flatView(excludePredicate: (Gene) -> Boolean = {false}): List<Gene>{
        return listOf(this)
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    abstract fun containsSameValueAs(other: Gene): Boolean


    /**
     * @return internal genes
     */
    abstract fun innerGene() : List<Gene>

    /**
     * evaluate whether [this] and [gene] belong to one evolution during search
     */
    open fun possiblySame(gene : Gene) : Boolean = gene.name == name && gene::class == this::class


    //========================= handing binding genes ===================================

    private val bindingGenes: MutableSet<Gene> = mutableSetOf()

    /**
     * rebuild the binding relationship of [this] gene based on [copiedGene] which exists in [copiedIndividual]
     */
    fun rebuildBindingWithTemplate(newIndividual: Individual, copiedIndividual: Individual, copiedGene: Gene){
        if (bindingGenes.isNotEmpty())
            throw IllegalArgumentException("gene ($name) has been rebuilt")

        val list = copiedGene.bindingGenes.map { g->
            newIndividual.findGene(copiedIndividual, g)
                ?:throw IllegalArgumentException("cannot find the gene (${g.name}) in the copiedIndividual")
        }

        bindingGenes.addAll(list)
    }

    /**
     * sync [bindingGenes] based on [this]
     */
    fun syncBindingGenesBasedOnThis(all : MutableSet<Gene> = mutableSetOf()){
        if (bindingGenes.isEmpty()) return
        all.add(this)
        bindingGenes.filterNot { all.contains(it) }.forEach { b->
            all.add(b)
            if(!b.bindValueBasedOn(this))
                LoggingUtil.uniqueWarn(log, "fail to bind the gene (${b.name} with the type ${b::class.java.simpleName}) based on this gene (${this.name} with ${this::class.java.simpleName})")
            b.syncBindingGenesBasedOnThis(all)
        }

        innerGene().filterNot { all.contains(it) }.forEach { it.syncBindingGenesBasedOnThis(all) }
    }

    /**
     * @return whether [this] gene is bound with any other gene
     */
    fun isBoundGene() = bindingGenes.isNotEmpty()

    /**
     * repair the broken binding reference e.g., the binding gene is removed from the current individual
     */
    fun cleanBrokenReference(all : List<Gene>) : Boolean{
        return bindingGenes.removeIf { !all.contains(it) }
    }

    /**
     * add [gene] as the binding gene
     */
    fun addBindingGene(gene: Gene) {
        bindingGenes.add(gene)
    }

    /**
     * reset binding based on [genes]
     */
    fun resetBinding(genes: Set<Gene>) {
        bindingGenes.clear()
        bindingGenes.addAll(genes)
    }

    /**
     * @return whether [this] is bound with [gene]
     */
    fun isBoundWith(gene: Gene) = bindingGenes.contains(gene)


    /**
     * bind value of [this] gene based on [gene]
     * @return whether the binding performs successfully
     */
    abstract fun bindValueBasedOn(gene: Gene) : Boolean


    override fun postCopy(template: StructuralElement) {
        //rebuild the binding genes
        val root = getRoot()
        val postBinding = (template as Gene).bindingGenes.map {b->
            root.find(b) as? Gene?:throw IllegalStateException("mismatched type between template (${b::class.java.simpleName}) and found (Gene)")
        }
        bindingGenes.clear()
        bindingGenes.addAll(postBinding)

        super.postCopy(template)
    }

    /**
     * there might be a need to repair gene based on some constraints, e.g., DateGene and TimeGene
     */
    open fun repair(){
        //do nothing
    }
}

