package org.evomaster.core.search.gene

import org.apache.commons.lang3.StringEscapeUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.pow

object GeneUtils {

    private val log: Logger = LoggerFactory.getLogger(GeneUtils::class.java)


    /**
     * List where each element at position "i" has value "2^i"
     */
    val intpow2 = (0..30).map { 2.0.pow(it).toInt() }


    /**
     * The [EscapeMode] enum is here to clarify the supported types of Escape modes.
     *
     * Different purposes require different modes of escape (e.g. URI may require percent encoding). This is to
     * keep track of what modes are supported and how they map to the respective implementations.
     *
     * Any mode that is not supported will go under NONE, and will result in no escapes being applied at all. The
     * purpose is to ensure that, even if the mode being used is unsupported, the system will not throw an exception.
     * It may not behave as desired, but it should not crash.
     *
     */
    enum class EscapeMode {

        URI,
        SQL,
        ASSERTION,
        EXPECTATION,
        JSON,
        TEXT,
        XML,
        BODY,
        NONE,
        X_WWW_FORM_URLENCODED,
        BOOLEAN_SELECTION_MODE,
        BOOLEAN_SELECTION_NESTED_MODE,
        GQL_INPUT_MODE,
        GQL_INPUT_ARRAY_MODE,
        BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE,
        BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE,
        GQL_STR_VALUE
    }

    fun getDelta(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            range: Long = Long.MAX_VALUE,
            start: Int = intpow2.size,
            end: Int = 10
    ): Int {
        val maxIndex = apc.getExploratoryValue(start, end)

        var n = 0
        for (i in 0 until maxIndex) {
            n = i + 1
            if (intpow2[i] > range) {
                break
            }
        }

        //choose an i for 2^i modification
        val delta = randomness.chooseUpTo(intpow2, n)

        return delta
    }

    /**
     * Given a number [x], return its string representation, with padded 0s
     * to have a defined [length]
     */
    fun padded(x: Int, length: Int): String {

        require(length >= 0) { "Negative length" }

        val s = x.toString()

        require(length >= s.length) { "Value is too large for chosen length" }

        return if (x >= 0) {
            s.padStart(length, '0')
        } else {
            "-${(-x).toString().padStart(length - 1, '0')}"
        }
    }

    /**
     * When we generate data, we might want to generate invalid inputs
     * on purpose to stress out the SUT, ie for Robustness Testing.
     * But there are cases in which such kind of data makes no sense.
     * For example, when we initialize SQL data directly bypassing the SUT,
     * there is no point in having invalid data which will just make the SQL
     * commands fail with no effect.
     *
     * So, we simply "repair" such genes with only valid inputs.
     */
    fun repairGenes(genes: Collection<Gene>) {

        if (log.isTraceEnabled) {
            log.trace("repair genes {}", genes.joinToString(",") {
                //note that check whether the gene is printable is not enough here
                try {
                    it.getValueAsRawString()
                } catch (e: Exception) {
                    "null"
                }
            })
        }

        for (g in genes) {
            when (g) {
                /*
                    TODO, check with Andrea, why only DateGene and TimeGene?
                    there also exist a repair for StringGene
                 */
                is DateGene, is TimeGene -> g.repair()
            }
        }
    }


    /**
    [applyEscapes] - applies various escapes needed for assertion generation.
    Moved here to allow extension to other purposes (SQL escapes, for example) and to
    allow a more consistent way of making changes.

     * This includes escaping special chars for java and kotlin.
     * Currently, Strings containing "@" are split, on the assumption (somewhat premature, admittedly) that
     * the symbol signifies an object reference (which would likely cause the assertion to fail).
     * TODO: Tests are needed to make sure this does not break.
     * Escapes may have to be applied differently between:
     * Java and Kotlin
     * calls and assertions

     */

    fun applyEscapes(string: String, mode: EscapeMode = EscapeMode.NONE, format: OutputFormat): String {
        val ret = when (mode) {
            EscapeMode.URI -> applyUriEscapes(string, format)
            EscapeMode.SQL -> applySqlEscapes(string, format)
            EscapeMode.ASSERTION -> applyAssertionEscapes(string, format)
            EscapeMode.EXPECTATION -> applyExpectationEscapes(string, format)
            EscapeMode.JSON -> applyJsonEscapes(string, format)
            EscapeMode.TEXT -> applyTextEscapes(string, format)
            EscapeMode.NONE,
            EscapeMode.X_WWW_FORM_URLENCODED,
            EscapeMode.BOOLEAN_SELECTION_MODE,
            EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_MODE,
            EscapeMode.BOOLEAN_SELECTION_UNION_INTERFACE_OBJECT_FIELDS_MODE,
            EscapeMode.BOOLEAN_SELECTION_NESTED_MODE,
            EscapeMode.GQL_INPUT_ARRAY_MODE,
            EscapeMode.GQL_INPUT_MODE -> string
            EscapeMode.GQL_STR_VALUE -> applyGQLStr(string, format)
            EscapeMode.BODY -> applyBodyEscapes(string, format)
            EscapeMode.XML -> StringEscapeUtils.escapeXml(string)
        }
        //if(forQueries) return applyQueryEscapes(string, format)
        //else return applyAssertionEscapes(string, format)
        return ret
    }

    fun applyJsonEscapes(string: String, format: OutputFormat): String {
        val ret = string
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        return ret
    }

    /**
     * TODO might need a further handling based on [format]
     * Note that there is kind of post handling for graphQL, see [GraphQLUtils.getPrintableInputGenes]
     */
    fun applyGQLStr(string: String, format: OutputFormat) : String{
        val replace = string
            .replace("\"", "\\\\\"")

        return replace
    }

    fun applyExpectationEscapes(string: String, format: OutputFormat = OutputFormat.JAVA_JUNIT_4): String {
        val ret = string.replace("\\", """\\\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

        when {
            format.isKotlin() -> return ret.replace("\$", "\\\$")
            else -> return ret
        }
    }

    fun applyUriEscapes(string: String, format: OutputFormat): String {
        //val ret = URLEncoder.encode(string, "utf-8")
        val ret = string.replace("\\", "%5C")
                .replace("\"", "%22")
                .replace("\n", "%0A")

        if (format.isKotlin()) return ret.replace("\$", "%24")
        else return ret
    }

    fun applyTextEscapes(string: String, format: OutputFormat): String {
        val ret = string.replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        when {
            format.isKotlin() -> return ret.replace("\$", "\\\$")
            else -> return ret
        }

    }

    fun applyAssertionEscapes(string: String, format: OutputFormat): String {
        var ret = ""
        val timeRegEx = "[0-2]?[0-9]:[0-5][0-9]".toRegex()
        ret = string.split("@")[0] //first split off any reference that might differ between runs
                .split(timeRegEx)[0] //split off anything after specific timestamps that might differ
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
        else return ret
    }

    fun applyBodyEscapes(string: String, format: OutputFormat): String {
        var ret = string.replace("\\", """\\""")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\t", "\\t")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
                .replace("\\\\u", "\\u")
        //.replace("\$", "\${\'\$\'}")
        //ret.replace("\$", "\\\$")
        else return ret.replace("\\\\u", "\\u")

        /*
                   The \u denote unicode characters. For some reason, escaping the \\ leads to these being invalid.
                     Since they are valid in the back end (and they should, arguably, be possible), this leads to inconsistent behaviour.
                     This fix is a hack. It may be that some \u chars are not valid. E.g. \uAndSomeRubbish.

                     As far as I understand, the addition of an \ in the \unicode should not really happen.
                     They should be their own chars, and the .replace("\\", """\\""" should be fine, but for some reason
                     they are not.
                     */
    }

    fun applySqlEscapes(string: String, format: OutputFormat): String {
        val ret = string.replace("\\", """\\""")
                .replace("\"", "\\\\\"")

        if (format.isKotlin()) return ret.replace("\$", "\\\$")
        //.replace("\$", "\${\'\$\'}")
        //ret.replace("\$", "\\\$")
        else return ret
    }


    /**
     * Given an input gene, prevent any [CycleObjectGene] from affecting the phenotype.
     * For example, if [CycleObjectGene] is inside an [OptionalGene], then such gene
     * should never be selectable.
     * An array of [CycleObjectGene] would always be empty.
     * Etc.
     * However, it is not necessarily trivial. An [CycleObjectGene] might be required,
     * and so we would need to scan to its first ancestor in the tree which is an optional
     * or an array.
     *
     * [force] if true, throw exception if cannot prevent the cyclces
     */
    fun preventCycles(gene: Gene, force: Boolean = false) {

        val cycles = gene.flatView().filterIsInstance<CycleObjectGene>()
        if (cycles.isEmpty()) {
            //nothing to do
            return
        }

        for (c in cycles) {

            var p = c.parent
            loop@ while (p != null) {
                when (p) {
                    is OptionalGene -> {
                        p.forbidSelection(); break@loop
                    }
                    is ArrayGene<*> -> {
                        p.forceToOnlyEmpty(); break@loop
                    }
                    else -> p = p.parent
                }
            }

            if (p == null) {
                val msg = "Could not prevent cycle in ${gene.name} gene"
                if (force) {
                    throw RuntimeException(msg)
                }
                log.warn(msg)
            }
        }
    }

    fun hasNonHandledCycles(gene: Gene): Boolean {

        val cycles = gene.flatView().filterIsInstance<CycleObjectGene>()
        if (cycles.isEmpty()) {
            return false
        }

        for (c in cycles) {

            var p = c.parent
            loop@ while (p != null) {
                when {
                    (p is OptionalGene && !p.selectable) ||
                            (p is ArrayGene<*> && p.maxSize == 0)
                    -> {
                        break@loop
                    }
                    else -> p = p.parent
                }
            }

            if (p == null) {
                return true
            }
        }

        return false
    }

    /**
     * If the input gene is a root of a tree of genes (ie, it contains inside other genes),
     * then verify that the top ancestor of each child and their children is indeed this root.
     * Note: this is just testing for an invariant
     */
    fun verifyRootInvariant(gene: Gene): Boolean {

        if (gene.parent != null) {
            //not a root
            return true
        }

        val all = gene.flatView()
        if (all.size == 1) {
            //no child
            return true
        }

        for (g in all) {
            val root = g.getRoot()
            if (root != gene) {
                return false
            }
        }

        return true
    }


    /**
     * In some cases, in particular GraphQL, given an object we might want to specify
     * just which fields we want to have, which is a boolean selection (ie, either a filed should
     * be present, or not). But we need to handle this recursively, because an object could have
     * objects inside, and so on recursively.
     *
     * However, to be able to print such selection for GraphQL, we need then to have a special mode
     * for its string representation.
     *
     * Also, we need to deal for when elements are non-nullable vs. nullable.
     */
    fun getBooleanSelection(gene: Gene): ObjectGene {

        if (shouldApplyBooleanSelection(gene)) {
            val selectedGene = handleBooleanSelection(gene)
            if (selectedGene is OptionalGene) {
                return selectedGene.gene as ObjectGene
            } else {
                return selectedGene as ObjectGene
            }
        }
        throw IllegalArgumentException("Invalid input type: ${gene.javaClass}")
    }

    /**
     * force at least one boolean to be selected
     */
    fun repairBooleanSelection(obj: ObjectGene) {

        if (obj.fields.isEmpty()
                || obj.fields.count { it !is OptionalGene && it !is BooleanGene } > 0) {
            throw IllegalArgumentException("There should be at least 1 field, and they must be all optional or boolean")
        }

        val selected = obj.fields.filter { (it is OptionalGene && it.isActive) || (it is BooleanGene && it.value) }

        if (selected.isNotEmpty()) {
            //it is fine, but we still need to make sure selected objects are fine
            selected.forEach {
                if (it is OptionalGene && it.gene is ObjectGene && it.gene !is CycleObjectGene) {
                    repairBooleanSelection(it.gene)
                }
            }
        } else {
            //must select at least one

            val candidates = obj.fields.filter { (it is OptionalGene && it.selectable) || it is BooleanGene }
            assert(candidates.isNotEmpty())

            // maybe do at random?
            val selected = candidates[0]
            if (selected is OptionalGene) {
                selected.isActive = true
                if (selected.gene is ObjectGene) {
                    assert(selected.gene !is CycleObjectGene)
                    repairBooleanSelection(selected.gene)
                }
            } else {
                (selected as BooleanGene).value = true
            }
        }
    }

    fun shouldApplyBooleanSelection(gene: Gene) =
            (gene is OptionalGene && gene.gene is ObjectGene)
                    || gene is ObjectGene
                    || (gene is ArrayGene<*> && gene.template is ObjectGene)
                    || (gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is ObjectGene)
                    || (gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is ObjectGene)
                    || (gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is ObjectGene)

    private fun handleBooleanSelection(gene: Gene): Gene {

        return when (gene) {
            is OptionalGene -> {
                /*
                    this is nullable.
                    Any basic field will be represented with a BooleanGene (selected/unselected).
                    But for objects we need to use an Optional
                 */
                if (gene.gene is ObjectGene) {
                    OptionalGene(gene.name, handleBooleanSelection(gene.gene))
                } else {
                    if (gene.gene is ArrayGene<*>) {
                        handleBooleanSelection(gene.gene.template)
                    } else {
                        // on by default, but can be deselected during the search
                        BooleanGene(gene.name, true)
                    }
                }
            }
            is CycleObjectGene -> {
                gene
            }
            is ObjectGene -> {
                //need to look at each field
                ObjectGene(gene.name, gene.fields.map { handleBooleanSelection(it) })
            }
            is ArrayGene<*> -> handleBooleanSelection(gene.template)
            else -> {
                BooleanGene(gene.name, true)
            }
        }
    }


}

