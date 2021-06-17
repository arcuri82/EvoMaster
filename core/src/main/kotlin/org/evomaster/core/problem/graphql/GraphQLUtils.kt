package org.evomaster.core.problem.graphql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.*
import org.slf4j.LoggerFactory
import javax.ws.rs.client.Entity

object GraphQLUtils {

    private val log = LoggerFactory.getLogger(GraphQLUtils::class.java)

    fun generateGQLBodyEntity(a: GraphQLAction, targetFormat: OutputFormat): Entity<String>? {

        //TOdo check empty return type
        val returnGene = a.parameters.find { p -> p is GQReturnParam }?.gene

        val inputGenes = a.parameters.filterIsInstance<GQInputParam>().map { it.gene }

        var bodyEntity: Entity<String> = Entity.json(" ")


        if (a.methodType == GQMethodType.QUERY) {

            if (inputGenes.isNotEmpty()) {

                val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes, targetFormat)

                val printableInputGenes = getPrintableInputGenes(printableInputGene)

                //primitive type in Return
                bodyEntity = if (returnGene == null) {
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)         } ","variables":null}
                """.trimIndent())

                } else if (returnGene.name.endsWith(ObjectGene.unionTag)) {//The first is a union type

                    var query = getQuery(returnGene, a)//todo remove the name for the first union
                    Entity.json("""
                   {"query" : " {  ${a.methodName} ($printableInputGenes)  { $query }  }   ","variables":null}
                """.trimIndent())

                } else {
                    val query = getQuery(returnGene, a)
                    Entity.json("""
                    {"query" : "  { ${a.methodName}  ($printableInputGenes)  $query       } ","variables":null}
                """.trimIndent())

                }
            } else {//request without arguments
                bodyEntity = if (returnGene == null) { //primitive type
                    Entity.json("""
                    {"query" : "  { ${a.methodName}       } ","variables":null}
                """.trimIndent())

                }
              /*  else if (returnGene.name.endsWith(ObjectGene.unionTag)|| returnGene.name.endsWith(ObjectGene.interfaceTag)) {//The first one is a union

                    var query = getQuery(returnGene, a)
                    Entity.json("""
                   {"query" : "    { $query }     ","variables":null}
                """.trimIndent())

                } */

                else {
                    var query = getQuery(returnGene, a)
                    Entity.json("""
                   {"query" : " {  ${a.methodName}  $query   }   ","variables":null}
                """.trimIndent())
                }
            }
        } else if (a.methodType == GQMethodType.MUTATION) {
            val printableInputGene: MutableList<String> = getPrintableInputGene(inputGenes, targetFormat)

            val printableInputGenes = getPrintableInputGenes(printableInputGene)

            /*
                Need a check with Asma
                for mutation which does not have any param, there is no need for ()
                e.g., createX:X!
                      mutation{
                        createX{
                            ...
                        }
                      }
             */
            val inputParams = if (printableInputGene.isEmpty()) "" else "($printableInputGenes)"
            bodyEntity = if (returnGene == null) {//primitive type
                Entity.json("""
                {"query" : " mutation{ ${a.methodName}  $inputParams         } ","variables":null}
            """.trimIndent())

            } else {
                val mutation = getMutation(returnGene, a)
                Entity.json("""
                { "query" : "mutation{    ${a.methodName}  $inputParams    $mutation    }","variables":null}
            """.trimIndent())

            }
        } else {
            LoggingUtil.uniqueWarn(log, " method type not supported yet : ${a.methodType}")
            return null
        }
        return bodyEntity
    }

    fun getMutation(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }

    fun getQuery(returnGene: Gene, a: GraphQLAction): String {
        return returnGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)
    }


    fun getPrintableInputGenes(printableInputGene: MutableList<String>): String {
        // Man: is the fun to handle " in printable
        return printableInputGene.joinToString(",").replace("\"", "\\\"")

    }

    fun getPrintableInputGene(inputGenes: List<Gene>, targetFormat: OutputFormat? = null): MutableList<String> {
        val printableInputGene = mutableListOf<String>()
        for (gene in inputGenes) {
            if (gene is EnumGene<*> || (gene is OptionalGene && gene.gene is EnumGene<*>)) {
                val i = gene.getValueAsRawString()
                printableInputGene.add("${gene.name} : $i")
            } else {
                if (gene is ObjectGene || (gene is OptionalGene && gene.gene is ObjectGene)) {
                    val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_MODE)
                    printableInputGene.add(" $i")
                } else {
                    if (gene is ArrayGene<*> || (gene is OptionalGene && gene.gene is ArrayGene<*>)) {
                        val i = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.GQL_INPUT_ARRAY_MODE)
                        printableInputGene.add("${gene.name} : $i")
                    } else {
                        /*
                            TODO
                            Man: this is a temporal solution
                            there might also need a further handling, e.g., field of object is String, Array<String>
                         */
                        val mode = if (ParamUtil.getValueGene(gene) is StringGene) GeneUtils.EscapeMode.GQL_STR_VALUE else GeneUtils.EscapeMode.GQL_INPUT_MODE
                        val i = gene.getValueAsPrintableString(mode = mode, targetFormat = targetFormat)
                        printableInputGene.add("${gene.name} : $i")
                    }
                }
            }
        }
        return printableInputGene
    }


    fun repairIndividual(ind: GraphQLIndividual) {
        ind.seeActions().forEach { a ->
            a.parameters.filterIsInstance<GQReturnParam>().forEach { p ->
                if (p.gene is ObjectGene) {
                    GeneUtils.repairBooleanSelection(p.gene)
                }
            }
        }
    }

}