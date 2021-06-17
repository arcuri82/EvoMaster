package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.impact.impactinfocollection.GeneImpact
import org.evomaster.core.search.impact.impactinfocollection.value.ObjectGeneImpact
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder

/**
 * @property refType presents the name of reference type of the object
 */
open class ObjectGene(name: String, val fields: List<out Gene>, val refType: String? = null) : Gene(name, mutableListOf<StructuralElement>().apply { addAll(fields) }) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ObjectGene::class.java)

        val unionTag = "#UNION#"
        val interfaceTag = "#INTERFACE#"
    }

    override fun getChildren(): List<Gene> {
        return fields
    }

    override fun copyContent(): Gene {
        return ObjectGene(name, fields.map(Gene::copyContent), refType)
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        for (i in 0 until fields.size) {
            this.fields[i].copyValueFrom(other.fields[i])
        }
    }

    override fun isMutable(): Boolean {
        return fields.any { it.isMutable() }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is ObjectGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.fields.size == other.fields.size
                && this.fields.zip(other.fields) { thisField, otherField ->
            thisField.containsSameValueAs(otherField)
        }.all { it == true }
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        fields.filter { it.isMutable() }
                .forEach { it.randomize(randomness, forceNewValue, allGenes) }
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return fields.filter { it.isMutable() }
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        val buffer = StringBuffer()

        val includedFields = fields.filter {
            it !is CycleObjectGene && (it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene))
        }

        //by default, return in JSON format
        if (mode == null || mode == GeneUtils.EscapeMode.JSON) {
            buffer.append("{")

            includedFields.map {
                "\"${it.name}\":${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }.joinTo(buffer, ", ")

            buffer.append("}")

        } else if (mode == GeneUtils.EscapeMode.XML) {

            // TODO might have to handle here: <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            /*
                Note: this is a very basic support, which should not really depend
                much on. Problem is that we would need to access to the XSD schema
                to decide when fields should be represented with tags or attributes
             */

            buffer.append(openXml(name))
            includedFields.forEach {
                //FIXME put back, but then update all broken tests
                //buffer.append(openXml(it.name))
                buffer.append(it.getValueAsPrintableString(previousGenes, mode, targetFormat))
                //buffer.append(closeXml(it.name))
            }
            buffer.append(closeXml(name))

        } else if (mode == GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED) {

            buffer.append(includedFields.map {
                val name = URLEncoder.encode(it.getVariableName(), "UTF-8")
                val value = URLEncoder.encode(it.getValueAsRawString(), "UTF-8")
                "$name=$value"
            }.joinToString("&"))

        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE || mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE) {
            handleBooleanSelectionMode(includedFields, buffer, previousGenes, targetFormat, mode)
        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE) {
            handleUnionObjectSelection(includedFields, buffer, previousGenes, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE) {
            handleUnionFieldSelection(includedFields, buffer, previousGenes, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.GQL_INPUT_MODE) {
            handleInputSelection(buffer, includedFields, previousGenes, mode, targetFormat)
        } else if (mode == GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE) {
            handleInputArraySelection(buffer, includedFields, previousGenes, mode, targetFormat)
        } else {
            throw IllegalArgumentException("Unrecognized mode: $mode")
        }

        return buffer.toString()
    }

    private fun handleInputArraySelection(buffer: StringBuffer, includedFields: List<Gene>, previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?) {
        //GQL array in arguments need a special object printing mode form that differ from Json, Boolean selection and gql input modes:
        //without the obj name:  {FieldNName: instance }
        buffer.append("{")
        includedFields.map {
            when {
                (it is OptionalGene && it.gene is EnumGene<*>) || it is EnumGene<*> -> "${it.name}:${it.getValueAsRawString()}"
                else -> "${it.name}:${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
            }
        }.joinTo(buffer, ", ")
        buffer.append("}")
    }

    private fun handleInputSelection(buffer: StringBuffer, includedFields: List<Gene>, previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?) {
        //GQL arguments need a special object printing mode form that differ from Json and Boolean selection:
        //ObjName:{FieldNName: instance }
        buffer.append("$name")
        buffer.append(":{")

        includedFields.map {
            "${it.name}:${it.getValueAsPrintableString(previousGenes, mode, targetFormat)}"
        }.joinTo(buffer, ", ")

        buffer.append("}")
    }

    private fun handleUnionFieldSelection(includedFields: List<Gene>, buffer: StringBuffer, previousGenes: List<Gene>, targetFormat: OutputFormat?) {
        /*For GraphQL we need UNION OBJECT FIELDS MODE to print out object`s fields in the union type eg:
               ... on UnionObject1 {
                fields<----
           }
               ... on UnionObjectN {
                fields<----
           }
       */
        val selection = includedFields.filter {
            when (it) {
                is OptionalGene -> it.isActive
                is ObjectGene -> true // TODO check if should skip if none of its subfield is selected
                is BooleanGene -> it.value
                else -> throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
            }
        }

        buffer.append(selection.map {
            val s: String = when (it) {
                is OptionalGene -> {
                    assert(it.gene is ObjectGene)
                    it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE, targetFormat)
                }
                is ObjectGene -> {//todo check
                    it.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE, targetFormat)
                }
                is BooleanGene -> {
                    it.name
                }
                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        }.joinToString(","))

    }

    private fun handleUnionObjectSelection(includedFields: List<Gene>, buffer: StringBuffer, previousGenes: List<Gene>, targetFormat: OutputFormat?) {
        /*For GraphQL we need UNION OBJECT MODE to print out the objects in the union type eg:
                ... on UnionObject1 {<----------
                fields
            }
                ... on UnionObjectN {<----------
                 fields
            }
             */
        val selection = includedFields.filter {
            when (it) {
                is OptionalGene -> it.isActive
                else -> throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
            }
        }
        selection.map {
            val s: String = when (it) {
                is OptionalGene -> {
                     buffer.append("... on ${it.gene.name.replace(unionTag, "")} {")
                    assert(it.gene is ObjectGene)
                    buffer.append("${it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE, targetFormat)}")
                    buffer.append("}").toString()
                }

                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        }.joinToString()
    }

    private fun handleBooleanSelectionMode(includedFields: List<Gene>, buffer: StringBuffer, previousGenes: List<Gene>, targetFormat: OutputFormat?, mode: GeneUtils.EscapeMode?) {

      //  if (includedFields.isEmpty()) {
      //      buffer.append("$name")
      //      return
      //  }

        if(name.endsWith(unionTag)){
            buffer.append("${name.replace(unionTag, " ")} {")
            buffer.append(getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE, targetFormat))
            buffer.append("}")
            return
        }

        if(name.endsWith(interfaceTag)){
            buffer.append("${name.replace(interfaceTag, " ")} {")
            buffer.append(getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE, targetFormat))
            buffer.append("}")
            return
        }

        if (mode == GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE) {
            //we do not do it for the first object, but we must do it for all the nested ones
            buffer.append("$name")
        }
        buffer.append("{")

        val selection = includedFields.filter {
            when (it) {
                is OptionalGene -> it.isActive
                is ObjectGene -> true // TODO check if should skip if none of its subfield is selected
                is BooleanGene -> it.value
                else -> throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
            }
        }

        buffer.append(selection.map {
            val s: String = when (it) {
                is OptionalGene -> {
                    assert(it.gene is ObjectGene)
                    it.gene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE, targetFormat)
                }
                is ObjectGene -> {
                    it.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.BOOLEAN_SELECTION_NESTED_MODE, targetFormat)
                }
                is BooleanGene -> {
                    it.name
                }
                else -> {
                    throw RuntimeException("BUG in EvoMaster: unexpected type ${it.javaClass}")
                }
            }
            s
        }.joinToString(","))
        buffer.append("}")
    }
//        }
    // }

    private fun openXml(tagName: String) = "<$tagName>"

    private fun closeXml(tagName: String) = "</$tagName>"


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(fields.flatMap { g -> g.flatView(excludePredicate) })
    }

    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {

        if (additionalGeneMutationInfo.impact != null
                && additionalGeneMutationInfo.impact is ObjectGeneImpact) {
            val impacts = internalGenes.map { additionalGeneMutationInfo.impact.fields.getValue(it.name) }
            val selected = mwc.selectSubGene(
                    internalGenes, true, additionalGeneMutationInfo.targets, individual = null, impacts = impacts, evi = additionalGeneMutationInfo.evi
            )
            val map = selected.map { internalGenes.indexOf(it) }
            return map.map { internalGenes[it] to additionalGeneMutationInfo.copyFoInnerGene(impact = impacts[it] as? GeneImpact, gene = internalGenes[it]) }
        }
        throw IllegalArgumentException("impact is null or not ObjectGeneImpact, ${additionalGeneMutationInfo.impact}")
    }


    override fun mutationWeight(): Double = fields.map { it.mutationWeight() }.sum()

    override fun innerGene(): List<Gene> = fields


    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is ObjectGene &&  (fields.indices).all { fields[it].possiblySame(gene.fields[it])}){
            var result = true
            (fields.indices).forEach{
                val r = fields[it].bindValueBasedOn(gene.fields[it])
                if (!r)
                    LoggingUtil.uniqueWarn(log, "cannot bind the field ${fields[it].name}")
                result = result && r
            }
            if (!result)
                LoggingUtil.uniqueWarn(log, "cannot bind the ${this::class.java.simpleName} (with the refType ${refType?:"null"}) with the object gene (with the refType ${gene.refType?:"null"})")
            return result
        }
        // might be cycle object genet
        LoggingUtil.uniqueWarn(log,"cannot bind the ${this::class.java.simpleName} (with the refType ${refType?:"null"}) with ${gene::class.java.simpleName}")
        return false
    }

}