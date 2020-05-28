package org.evomaster.core

import joptsimple.BuiltinHelpFormatter
import joptsimple.OptionDescriptor
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.impact.GeneMutationSelectionMethod
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType

typealias PercentageAsProbability = EMConfig.Probability

/**
 * Class used to hold all the main configuration properties
 * of EvoMaster.
 *
 */
class EMConfig {

    /*
        Code here does use the JOptSimple library

        https://pholser.github.io/jopt-simple/
     */

    companion object {

        fun validateOptions(args: Array<String>): OptionParser {

            val config = EMConfig()

            val parser = getOptionParser()
            val options = parser.parse(*args)

            if (!options.has("help")) {
                //actual validation is done here
                config.updateProperties(options)
            }

            return parser
        }

        /**
         * Having issue with types/kotlin/reflection...
         * Therefore, need custom output formatting.
         * However, easier way (for now) is to just override
         * what we want to change
         *
         *  TODO: groups and ordering
         */
        private class MyHelpFormatter : BuiltinHelpFormatter(80, 2) {
            override fun extractTypeIndicator(descriptor: OptionDescriptor): String? {
                return null
            }
        }

        /**
         * Get all available "console options" for the annotated properties
         */
        fun getOptionParser(): OptionParser {

            val defaultInstance = EMConfig()

            var parser = OptionParser()

            parser.accepts("help", "Print this help documentation")
                    .forHelp()

            getConfigurationProperties().forEach { m ->
                /*
                    Note: here we could use typing in the options,
                    instead of converting everything to string.
                    But it looks bit cumbersome to do it in Kotlin,
                    at least for them moment

                    Until we make a complete MyHelpFormatter, here
                    for the types we "hack" the default one, ie,
                    we set the type description to "null", but then
                    the argument description will contain the type
                 */

                val argTypeName = m.returnType.toString()
                        .run { substring(lastIndexOf('.') + 1) }

                parser.accepts(m.name, getDescription(m).toString())
                        .withRequiredArg()
                        .describedAs(argTypeName)
                        .defaultsTo(m.call(defaultInstance).toString())
            }

            parser.formatHelpWith(MyHelpFormatter())

            return parser
        }

        class ConfigDescription(
                val text: String,
                val constraints: String,
                val enumValues: String,
                val experimental: Boolean
        ) {
            override fun toString(): String {
                var description = text
                if (constraints.isNotBlank()) {
                    description += " [Constraints: $constraints]."
                }
                if (enumValues.isNotBlank()) {
                    description += " [Values: $enumValues]."
                }

                if (experimental) {
                    /*
                    TODO: For some reasons, coloring is not working here.
                    Could open an issue at:
                    https://github.com/jopt-simple/jopt-simple
                    */
                    //description = AnsiColor.inRed("EXPERIMENTAL: $description")
                    description = "EXPERIMENTAL: $description"
                }
                return description
            }
        }

        fun getDescription(m: KMutableProperty<*>): ConfigDescription {

            val cfg = (m.annotations.find { it is Cfg } as? Cfg)
                    ?: throw IllegalArgumentException("Property ${m.name} is not annotated with @Cfg")

            val text = cfg.description.trim().run {
                when {
                    isBlank() -> "No description."
                    !endsWith(".") -> "$this."
                    else -> this
                }
            }

            val min = (m.annotations.find { it is Min } as? Min)?.min
            val max = (m.annotations.find { it is Max } as? Max)?.max
            val probability = m.annotations.find { it is Probability }
            val url = m.annotations.find { it is Url }
            val regex = (m.annotations.find { it is Regex } as? Regex)?.regex

            var constraints = ""
            if (min != null || max != null || probability != null || url != null || regex != null) {
                if (min != null) {
                    constraints += "min=$min"
                }
                if (max != null) {
                    if (min != null) constraints += ", "
                    constraints += "max=$max"
                }
                if (probability != null) {
                    constraints += "probability 0.0-1.0"
                }
                if (url != null) {
                    constraints += "URL"
                }
                if (regex != null) {
                    constraints += "regex $regex"
                }
            }

            var enumValues = ""

            val returnType = m.returnType.javaType as Class<*>

            if (returnType.isEnum) {
                val elements = returnType.getDeclaredMethod("values")
                        .invoke(null) as Array<*>

                enumValues = elements.joinToString(", ")
            }

            var description = "$text$constraints$enumValues"

            val experimental = (m.annotations.find { it is Experimental } as? Experimental)

            val cd = ConfigDescription(text, constraints, enumValues, experimental != null)

            return cd
        }


        fun getConfigurationProperties(): List<KMutableProperty<*>> {
            return EMConfig::class.members
                    .filterIsInstance(KMutableProperty::class.java)
                    .filter { it.annotations.any { it is Cfg } }
        }
    }

    /**
     * Update the values of the properties based on the options
     * chosen on the command line
     *
     *
     * @throws IllegalArgumentException if there are constraint violations
     */
    fun updateProperties(options: OptionSet) {

        getConfigurationProperties().forEach { m ->

            updateProperty(options, m)

            checkPropertyConstraints(m)
        }

        checkMultiFieldConstraints()
    }

    private fun checkMultiFieldConstraints() {
        /*
            Each option field might have specific constraints, setup with @annotations.
            However, there can be multi-field constraints as well.
            Those are defined here.
            They can be check only once all fields have been updated
         */

        when (stoppingCriterion) {
            StoppingCriterion.TIME -> if (maxActionEvaluations != defaultMaxActionEvaluations) {
                throw IllegalArgumentException("Changing number of max actions, but stopping criterion is time")
            }
            StoppingCriterion.FITNESS_EVALUATIONS -> if (maxTimeInSeconds != defaultMaxTimeInSeconds ||
                    maxTime != defaultMaxTime) {
                throw IllegalArgumentException("Changing max time, but stopping criterion is based on fitness evaluations")
            }
        }

        if (shouldGenerateSqlData() && !heuristicsForSQL) {
            throw IllegalArgumentException("Cannot generate SQL data if you not enable " +
                    "collecting heuristics with 'heuristicsForSQL'")
        }

        if (heuristicsForSQL && !extractSqlExecutionInfo) {
            throw IllegalArgumentException("Cannot collect heuristics SQL data if you not enable " +
                    "extracting SQL execution info with 'extractSqlExecutionInfo'")
        }

        if (shouldGenerateMongoData() && !heuristicsForMongo) {
            throw IllegalArgumentException("Cannot generate MongoDB data if you not enable " +
                    "collecting heuristics with 'heuristicsForMongo'")
        }

        if (heuristicsForMongo && !extractMongoExecutionInfo) {
            throw IllegalArgumentException("Cannot collect heuristics Mongo data if you not enable " +
                    "extracting Mongo execution info with 'extractMongoExecutionInfo'")
        }

        if (enableTrackEvaluatedIndividual && enableTrackIndividual) {
            throw IllegalArgumentException("When tracking EvaluatedIndividual, it is not necessary to track individual")
        }

        //resource related parameters
        if ((resourceSampleStrategy != ResourceSamplingStrategy.NONE || (probOfApplySQLActionToCreateResources > 0.0) || doesApplyNameMatching || probOfEnablingResourceDependencyHeuristics > 0.0 || exportDependencies)
                && (problemType != ProblemType.REST || algorithm != Algorithm.MIO)) {
            throw IllegalArgumentException("Parameters (${
            arrayOf("resourceSampleStrategy", "probOfApplySQLActionToCreateResources", "doesApplyNameMatching", "probOfEnablingResourceDependencyHeuristics", "exportDependencies")
                    .filterIndexed { index, _ ->
                        (index == 0 && resourceSampleStrategy != ResourceSamplingStrategy.NONE) ||
                                (index == 1 && (probOfApplySQLActionToCreateResources > 0.0)) ||
                                (index == 2 && doesApplyNameMatching) ||
                                (index == 3 && probOfEnablingResourceDependencyHeuristics > 0.0) ||
                                (index == 4 && exportDependencies)
                    }.joinToString(" and ")
            }) are only applicable on REST problem (but current is $problemType) with MIO algorithm (but current is $algorithm).")
        }

        /*
            resource-mio and sql configuration
            TODO if required
         */
        if (resourceSampleStrategy != ResourceSamplingStrategy.NONE && (heuristicsForSQL || generateSqlDataWithSearch || generateSqlDataWithDSE || geneMutationStrategy == GeneMutationStrategy.ONE_OVER_N)) {
            throw IllegalArgumentException(" resource-mio does not support SQL strategies for the moment")
        }

        //archive-based mutation
        if (geneSelectionMethod != GeneMutationSelectionMethod.NONE && algorithm != Algorithm.MIO) {
            throw IllegalArgumentException("GeneMutationSelectionMethod is only applicable with MIO algorithm (but current is $algorithm)")
        }

        if (baseTaintAnalysisProbability > 0 && !useMethodReplacement) {
            throw IllegalArgumentException("Base Taint Analysis requires 'useMethodReplacement' option")
        }

        if (blackBox && !bbExperiments) {
            if (problemType == ProblemType.REST && bbSwaggerUrl.isNullOrBlank()) {
                throw IllegalArgumentException("In black-box mode for REST APIs, you need to set the bbSwaggerUrl option")
            }
            if (outputFormat == OutputFormat.DEFAULT) {
                throw IllegalArgumentException("In black-box mode, you must specify a value for the outputFormat option different from DEFAULT")
            }
        }

        if (!blackBox && bbExperiments) {
            throw IllegalArgumentException("Cannot setup bbExperiments without black-box mode")
        }

        if (testSuiteFileName.contains("-") && outputFormat.isJavaOrKotlin()) {
            throw IllegalArgumentException("In JVM languages, you cannot use the symbol '-' in test suite file name")
        }
    }

    private fun checkPropertyConstraints(m: KMutableProperty<*>) {
        val parameterValue = m.getter.call(this).toString()

        //check value against constraints on its field, if any
        m.annotations.find { it is Min }?.also {
            it as Min
            if (parameterValue.toDouble() < it.min) {
                throw IllegalArgumentException("Failed to handle Min ${it.min} constraint for" +
                        " parameter '${m.name}' with value $parameterValue")
            }
        }

        m.annotations.find { it is Max }?.also {
            it as Max
            if (parameterValue.toDouble() > it.max) {
                throw IllegalArgumentException("Failed to handle Max ${it.max} constraint for" +
                        " parameter '${m.name}' with value $parameterValue")
            }
        }

        m.annotations.find { it is Probability }?.also {
            it as Probability
            val p = parameterValue.toDouble()
            if (p < 0 || p > 1) {
                throw IllegalArgumentException("Failed to handle probability constraint for" +
                        " parameter '${m.name}' with value $parameterValue. The value must be in [0,1].")
            }
        }

        m.annotations.find { it is Url }?.also {
            if (!parameterValue.isNullOrBlank()) {
                try {
                    URL(parameterValue)
                } catch (e: MalformedURLException) {
                    throw IllegalArgumentException("Parameter '${m.name}' with value $parameterValue is" +
                            " not a valid URL: ${e.message}")
                }
            }
        }

        m.annotations.find { it is Regex }?.also {
            it as Regex
            if (!parameterValue.matches(kotlin.text.Regex(it.regex))) {
                throw IllegalArgumentException("Parameter '${m.name}' with value $parameterValue is" +
                        " not matching the regex: ${it.regex}")
            }
        }

        m.annotations.find { it is Folder }?.also{
            val path = try{
                Paths.get(parameterValue).toAbsolutePath()
            } catch(e: InvalidPathException){
                throw IllegalArgumentException("Parameter '${m.name}' is not a valid FS path: ${e.message}")
            }

            if(Files.exists(path) && ! Files.isWritable(path)){
                throw IllegalArgumentException("Parameter '${m.name}' refers to a folder that already" +
                        " exists, but that cannot be written to: $path")
            }
            if(Files.exists(path) && ! Files.isDirectory(path)){
                throw IllegalArgumentException("Parameter '${m.name}' refers to a file that already" +
                        " exists, but that it is not a folder: $path")
            }
        }

        m.annotations.find { it is FilePath }?.also{
            val path = try{
                Paths.get(parameterValue).toAbsolutePath()
            } catch(e: InvalidPathException){
                throw IllegalArgumentException("Parameter '${m.name}' is not a valid FS path: ${e.message}")
            }

            if(Files.exists(path) && ! Files.isWritable(path)){
                throw IllegalArgumentException("Parameter '${m.name}' refers to a file that already" +
                        " exists, but that cannot be written/replace to: $path")
            }
            if(Files.exists(path) && Files.isDirectory(path)){
                throw IllegalArgumentException("Parameter '${m.name}' refers to a file that is instead an" +
                        " existing folder: $path")
            }
        }
    }

    private fun updateProperty(options: OptionSet, m: KMutableProperty<*>) {
        val opt = options.valueOf(m.name)?.toString()
                ?: throw IllegalArgumentException("Value not found for property ${m.name}")

        val returnType = m.returnType.javaType as Class<*>

        /*
                TODO: ugly checks. But not sure yet if can be made better in Kotlin.
                Could be improved with isSubtypeOf from 1.1?
                http://stackoverflow.com/questions/41553647/kotlin-isassignablefrom-and-reflection-type-checks
             */

        //update value, but only if it was in the specified options.
        //WARNING: without this check, it would reset to default for fields not in "options"
        if (options.has(m.name)) {
            try {
                if (Integer.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, Integer.parseInt(opt))

                } else if (java.lang.Long.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Long.parseLong(opt))

                } else if (java.lang.Double.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Double.parseDouble(opt))

                } else if (java.lang.Boolean.TYPE.isAssignableFrom(returnType)) {
                    m.setter.call(this, java.lang.Boolean.parseBoolean(opt))

                } else if (java.lang.String::class.java.isAssignableFrom(returnType)) {
                    m.setter.call(this, opt)

                } else if (returnType.isEnum) {
                    val valueOfMethod = returnType.getDeclaredMethod("valueOf",
                            java.lang.String::class.java)
                    m.setter.call(this, valueOfMethod.invoke(null, opt))

                } else {
                    throw IllegalStateException("BUG: cannot handle type $returnType")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to handle property '${m.name}'", e)
            }
        }
    }

    fun shouldGenerateSqlData() = generateSqlDataWithDSE || generateSqlDataWithSearch

    fun shouldGenerateMongoData() = generateMongoDataWithSearch

    fun experimentalFeatures(): List<String> {

        val properties = getConfigurationProperties()
                .filter { it.annotations.find { it is Experimental } != null }
                .filter {
                    val returnType = it.returnType.javaType as Class<*>
                    when {
                        java.lang.Boolean.TYPE.isAssignableFrom(returnType) -> it.getter.call(this) as Boolean
                        it.annotations.find { p -> p is Probability && p.activating } != null -> (it.getter.call(this) as Double) > 0
                        else -> false
                    }
                }
                .map { it.name }

        val enums = getConfigurationProperties()
                .filter {
                    val returnType = it.returnType.javaType as Class<*>
                    if (returnType.isEnum) {
                        val e = it.getter.call(this)
                        val f = returnType.getField(e.toString())
                        f.annotations.find { it is Experimental } != null
                    } else {
                        false
                    }
                }
                .map { "${it.name}=${it.getter.call(this)}" }

        return properties.plus(enums)
    }

//------------------------------------------------------------------------
//--- custom annotations

    /**
     * Configuration (CFG in short) for EvoMaster.
     * Properties annotated with [Cfg] can be set from
     * command line.
     * The code in this class uses reflection, on each property
     * marked with this annotation, to build the list of available
     * modifiable configurations.
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Cfg(val description: String)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Min(val min: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Max(val max: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Url

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Regex(val regex: String)


    /**
     * A double value between 0 and 1
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Probability(
            /**
             * Specify if this probability would activate a functionality if greater than 0.
             * If not, it might still not be used, depending on other configurations.
             * This is mainly needed when dealing with @Experimental probabilities that must
             * be put to 0 if they would activate a new feature that is still unstable
             */
            val activating: Boolean = true
    )


    /**
     * This annotation is used to represent properties controlling
     * features that are still work in progress.
     * Do not use them (yet) in production.
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    @MustBeDocumented
    annotation class Experimental


    /**
     * This represent one of the main properties to set in EvoMaster.
     * Those are the ones most likely going to be set by practitioners.
     * Note: most of the other properties are mainly for experiments
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Important(
            /**
             * The lower value, the more importance.
             * This only impact of options are sorted when displayed
             */
            val priority: Double
    )

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Folder

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class FilePath

//------------------------------------------------------------------------
//--- properties

    /*
        WARNING
        if any change is made here, the "options.md" MUST be recreated
        with ConfigToMarkdown

        You will also need to check if any special character you use in the
        descriptions end-up in some screwed-up Markdown layout
     */

    //----- "Important" options, sorted by priority --------------

    val defaultMaxTime = "60s"

    @Important(1.0)
    @Cfg("Maximum amount of time allowed for the search. " +
            " The time is expressed with a string where hours (`h`), minutes (`m`) and" +
            " seconds (`s`) can be specified, e.g., `1h10m120s` and `72m` are both valid" +
            " and equivalent." +
            " Each component (i.e., `h`, `m` and `s`) is optional, but at least one must be specified. " +
            " In other words, if you need to run the search for just `30` seconds, you can write `30s` " +
            " instead of `0h0m30s`." +
            " **The more time is allowed, the better results one can expect**." +
            " But then of course the test generation will take longer." +
            " For how long should _EvoMaster_ be left run?" +
            " The default 1 _minute_ is just for demonstration." +
            " __We recommend to run it between 1 and 24 hours__, depending on the size and complexity " +
            " of the tested application."
    )
    @Regex("(\\s*)((?=(\\S+))(\\d+h)?(\\d+m)?(\\d+s)?)(\\s*)")
    var maxTime = defaultMaxTime

    @Important(2.0)
    @Cfg("The path directory of where the generated test classes should be saved to")
    @Folder
    var outputFolder = "src/em"


    @Important(2.0)
    @Cfg("The name of generated file with the test cases, without file type extension." +
            " In JVM languages, if the name contains '.', folders will be created to represent" +
            " the given package structure." +
            " Also, in JVM languages, should not use '-' in the file name, as not valid symbol" +
            " for class identifiers.")
    @Regex("[-a-zA-Z\$_][-0-9a-zA-Z\$_]*(.[-a-zA-Z\$_][-0-9a-zA-Z\$_]*)*")
    var testSuiteFileName = "EvoMasterTest"

    @Important(2.0)
    @Cfg("Specify in which format the tests should be outputted." +
            " If left on `DEFAULT`, then the value specified in the _EvoMaster Driver_ will be used." +
            " But a different value must be chosen if doing Black-Box testing.")
    var outputFormat = OutputFormat.DEFAULT

    @Important(3.0)
    @Cfg("Use EvoMaster in black-box mode. This does not require an EvoMaster Driver up and running. However, you will need to provide further option to specify how to connect to the SUT")
    var blackBox = false

    @Important(3.2)
    @Url
    @Cfg("When in black-box mode for REST APIs, specify where the Swagger schema can be downloaded from")
    var bbSwaggerUrl: String = ""

    @Important(3.5)
    @Url
    @Cfg("When in black-box mode, specify the URL of where the SUT can be reached. " +
            "If this is missing, the URL will be inferred from Swagger.")
    var bbTargetUrl: String = ""


    //-------- other options -------------


    @Cfg("At times, we need to run EvoMaster with printed logs that are deterministic." +
            " For example, this means avoiding printing out time-stamps.")
    var avoidNonDeterministicLogs = false

    enum class Algorithm {
        MIO, RANDOM, WTS, MOSA
    }

    @Cfg("The algorithm used to generate test cases")
    var algorithm = Algorithm.MIO


    enum class ProblemType {
        REST,
        @Experimental
        WEB
    }

    @Cfg("The type of SUT we want to generate tests for, e.g., a RESTful API")
    var problemType = ProblemType.REST


    @Cfg("Specify if test classes should be created as output of the tool. " +
            "Usually, you would put it to 'false' only when debugging EvoMaster itself")
    var createTests = true


    enum class TestSuiteSplitType {
        NONE,
        CLUSTER,
        SUMMARY_ONLY,
        CODE
    }

    @Experimental
    @Cfg("Instead of generating a single test file, it could be split in several files, according to different strategies")
    var testSuiteSplitType = TestSuiteSplitType.NONE

    @Experimental
    @Cfg("Generate an executive summary, containing an example of each category of potential fault found")
    var executiveSummary = false

    @Cfg("The seed for the random generator used during the search. " +
            "A negative value means the CPU clock time will be rather used as seed")
    var seed: Long = -1

    @Cfg("TCP port of where the SUT REST controller is listening on")
    @Min(0.0)
    @Max(65535.0)
    var sutControllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT

    @Cfg("Host name or IP address of where the SUT REST controller is listening on")
    var sutControllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST

    @Cfg("Limit of number of individuals per target to keep in the archive")
    @Min(1.0)
    var archiveTargetLimit = 10

    @Cfg("Probability of sampling a new individual at random")
    @Probability
    var probOfRandomSampling = 0.5

    @Cfg("The percentage of passed search before starting a more focused, less exploratory one")
    @PercentageAsProbability(true)
    var focusedSearchActivationTime = 0.5

    @Cfg("Number of applied mutations on sampled individuals, at the start of the search")
    @Min(0.0)
    var startNumberOfMutations = 1

    @Cfg("Number of applied mutations on sampled individuals, by the end of the search")
    @Min(0.0)
    var endNumberOfMutations = 10


    enum class StoppingCriterion {
        TIME,
        FITNESS_EVALUATIONS
    }

    @Cfg("Stopping criterion for the search")
    var stoppingCriterion = StoppingCriterion.TIME


    val defaultMaxActionEvaluations = 1000

    @Cfg("Maximum number of action evaluations for the search." +
            " A fitness evaluation can be composed of 1 or more actions," +
            " like for example REST calls or SQL setups." +
            " The more actions are allowed, the better results one can expect." +
            " But then of course the test generation will take longer." +
            " Only applicable depending on the stopping criterion.")
    @Min(1.0)
    var maxActionEvaluations = defaultMaxActionEvaluations


    val defaultMaxTimeInSeconds = 0

    @Cfg("Maximum number of seconds allowed for the search." +
            " The more time is allowed, the better results one can expect." +
            " But then of course the test generation will take longer." +
            " Only applicable depending on the stopping criterion." +
            " If this value is 0, the setting 'maxTime' will be used instead.")
    @Min(0.0)
    var maxTimeInSeconds = defaultMaxTimeInSeconds


    @Cfg("Whether or not writing statistics of the search process. " +
            "This is only needed when running experiments with different parameter settings")
    var writeStatistics = false

    @Cfg("Where the statistics file (if any) is going to be written (in CSV format)")
    @FilePath
    var statisticsFile = "statistics.csv"

    @Cfg("Whether should add to an existing statistics file, instead of replacing it")
    var appendToStatisticsFile = false

    @Cfg("If positive, check how often, in percentage % of the budget, to collect statistics snapshots." +
            " For example, every 5% of the time.")
    @Max(50.0)
    var snapshotInterval = -1.0

    @Cfg("Where the snapshot file (if any) is going to be written (in CSV format)")
    @FilePath
    var snapshotStatisticsFile = "snapshot.csv"

    @Cfg("An id that will be part as a column of the statistics file (if any is generated)")
    var statisticsColumnId = "-"

    @Cfg("Whether we should collect data on the extra heuristics. Only needed for experiments.")
    var writeExtraHeuristicsFile = false

    @Cfg("Where the extra heuristics file (if any) is going to be written (in CSV format)")
    @FilePath
    var extraHeuristicsFile = "extra_heuristics.csv"


    enum class SecondaryObjectiveStrategy {
        AVG_DISTANCE,
        AVG_DISTANCE_SAME_N_ACTIONS,
        BEST_MIN
    }

    @Cfg("Strategy used to handle the extra heuristics in the secondary objectives")
    var secondaryObjectiveStrategy = SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS

    @Cfg("Whether secondary objectives are less important than test bloat control")
    var bloatControlForSecondaryObjective = false

    @Cfg("Probability of applying a mutation that can change the structure of a test")
    @Probability
    var structureMutationProbability = 0.5


    enum class GeneMutationStrategy {
        ONE_OVER_N,
        ONE_OVER_N_BIASED_SQL
    }

    @Cfg("Strategy used to define the mutation probability")
    var geneMutationStrategy = GeneMutationStrategy.ONE_OVER_N_BIASED_SQL

    enum class FeedbackDirectedSampling {
        NONE,
        LAST,
        FOCUSED_QUICKEST
    }

    @Cfg("Specify whether when we sample from archive we do look at the most promising targets for which we have had a recent improvement")
    var feedbackDirectedSampling = FeedbackDirectedSampling.LAST

    @Cfg("Define the population size in the search algorithms that use populations (e.g., Genetic Algorithms, but not MIO)")
    @Min(1.0)
    var populationSize = 30

    @Cfg("Define the maximum number of tests in a suite in the search algorithms that evolve whole suites, e.g. WTS")
    @Min(1.0)
    var maxSearchSuiteSize = 50

    @Cfg("Probability of applying crossover operation (if any is used in the search algorithm)")
    @Probability
    var xoverProbability = 0.7

    @Cfg("Number of elements to consider in a Tournament Selection (if any is used in the search algorithm)")
    @Min(1.0)
    var tournamentSize = 10

    @Cfg("When sampling new test cases to evaluate, probability of using some smart strategy instead of plain random")
    @Probability
    var probOfSmartSampling = 0.5

    @Cfg("Max number of 'actions' (e.g., RESTful calls or SQL commands) that can be done in a single test")
    @Min(1.0)
    var maxTestSize = 10

    @Cfg("Tracking of SQL commands to improve test generation")
    var heuristicsForSQL = true

    @Cfg("Enable extracting SQL execution info")
    var extractSqlExecutionInfo = true

    @Experimental
    @Cfg("Tracking of MongoDB commands to improve test generation")
    var heuristicsForMongo = false

    @Experimental
    @Cfg("Enable extracting MongoDB execution info")
    var extractMongoExecutionInfo = false

    @Experimental
    @Cfg("Enable EvoMaster to generate MongoDB data with direct accesses to the database. Use a search algorithm")
    var generateMongoDataWithSearch = false

    @Experimental
    @Cfg("Enable EvoMaster to generate SQL data with direct accesses to the database. Use Dynamic Symbolic Execution")
    var generateSqlDataWithDSE = false

    @Cfg("Enable EvoMaster to generate SQL data with direct accesses to the database. Use a search algorithm")
    var generateSqlDataWithSearch = true

    @Cfg("When generating SQL data, how many new rows (max) to generate for each specific SQL Select")
    @Min(1.0)
    var maxSqlInitActionsPerMissingData = 5


    @Cfg("Maximum size (in bytes) that EM handles response payloads in the HTTP responses. " +
            "If larger than that, a response will not be stored internally in EM during the test generation. " +
            "This is needed to avoid running out of memory.")
    var maxResponseByteSize = 1_000_000

    @Cfg("Whether to print how much search done so far")
    var showProgress = true

    @Experimental
    @Cfg("Whether or not enable a search process monitor for archiving evaluated individuals and Archive regarding an evaluation of search. " +
            "This is only needed when running experiments with different parameter settings")
    var enableProcessMonitor = false

    @Experimental
    @Cfg("Specify a folder to save results when a search monitor is enabled")
    @Folder
    var processFiles = "process_data"

    @Experimental
    @Cfg("Specify how often to save results when a search monitor is enabled ")
    var processInterval = 100

    @Experimental
    @Cfg("Enable EvoMaster to generate, use, and attach complete objects to REST calls, rather than just the needed fields/values")
    var enableCompleteObjects = false

    @Experimental
    @Cfg("Whether to enable tracking the history of modifications of the individuals during the search")
    var enableTrackIndividual = false

    @Experimental
    @Cfg("Whether to enable tracking the history of modifications of the individuals with its fitness values (i.e., evaluated individual) during the search. " +
            "Note that we enforced that set enableTrackIndividual false when enableTrackEvaluatedIndividual is true since information of individual is part of evaluated individual")
    var enableTrackEvaluatedIndividual = false

    @Experimental
    @Cfg("Specify a maxLength of tracking when enableTrackIndividual or enableTrackEvaluatedIndividual is true. " +
            "Note that the value should be specified with a non-negative number or -1 (for tracking all history)")
    @Min(-1.0)
    var maxLengthOfTraces = 10

    @Cfg("Enable custom naming and sorting criteria")
    var customNaming = true

    /*
        You need to decode it if you want to know what it says...
     */
    @Cfg("QWN0aXZhdGUgdGhlIFVuaWNvcm4gTW9kZQ==")
    var e_u1f984 = false

    @Experimental
    @Cfg("Enable Expectation Generation. If enabled, expectations will be generated. " +
            "A variable called expectationsMasterSwitch is added to the test suite, with a default value of false. If set to true, an expectation that fails will cause the test case containing it to fail.")
    var expectationsActive = false

    @Cfg("Generate basic assertions. Basic assertions (comparing the returned object to itself) are added to the code. " +
            "NOTE: this should not cause any tests to fail.")
    var enableBasicAssertions = true

    @Cfg("Apply method replacement heuristics to smooth the search landscape")
    var useMethodReplacement = true

    @Cfg("Enable to expand the genotype of REST individuals based on runtime information missing from Swagger")
    var expandRestIndividuals = true

    enum class ResourceSamplingStrategy(val requiredArchive: Boolean = false) {
        NONE,
        /**
         * probability for applicable strategy is specified
         */
        Customized,
        /**
         * probability for applicable strategy is equal
         */
        EqualProbability,
        /**
         * probability for applicable strategy is derived based on actions
         */
        Actions,
        /**
         * probability for applicable strategy is adaptive with time
         */
        TimeBudgets,
        /**
         * probability for applicable strategy is adaptive with performance, i.e., Archive
         */
        Archive(true),
        /**
         * probability for applicable strategy is adaptive with performance, i.e., Archive
         */
        ConArchive(true)
    }

    @Experimental
    @Cfg("Specify whether to enable resource-based strategy to sample an individual during search. " +
            "Note that resource-based sampling is only applicable for REST problem with MIO algorithm.")
    var resourceSampleStrategy = ResourceSamplingStrategy.NONE

    @Experimental
    @Cfg("Specify whether to enable resource dependency heuristics, i.e, probOfEnablingResourceDependencyHeuristics > 0.0. " +
            "Note that the option is available to be enabled only if resource-based smart sampling is enable. " +
            "This option has an effect on sampling multiple resources and mutating a structure of an individual.")
    @Probability
    var probOfEnablingResourceDependencyHeuristics = 0.0

    @Experimental
    @Cfg("Specify whether to export derived dependencies among resources")
    var exportDependencies = false

    @Experimental
    @Cfg("Specify a file that saves derived dependencies")
    @FilePath
    var dependencyFile = "dependencies.csv"

    @Experimental
    @Cfg("Specify a probability to apply SQL actions for preparing resources for REST Action")
    @Probability
    var probOfApplySQLActionToCreateResources = 0.0

    @Experimental
    @Cfg("Specify a minimal number of rows in a table that enables selection (i.e., SELECT sql) to prepare resources for REST Action. " +
            "In other word, if the number is less than the specified, insertion is always applied.")
    @Min(0.0)
    var minRowOfTable = 10

    @Experimental
    @Cfg("Specify a probability that enables selection (i.e., SELECT sql) of data from database instead of insertion (i.e., INSERT sql) for preparing resources for REST actions")
    @Probability(false)
    var probOfSelectFromDatabase = 0.1

    @Experimental
    @Cfg("Whether to apply text/name analysis with natural language parser to derive relationships between name entities, e.g., a resource identifier with a name of table")
    var doesApplyNameMatching = false

    @Experimental
    @Cfg("Specify a probability to apply S1iR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S1iR: Double = 0.25

    @Experimental
    @Cfg("Specify a probability to apply S1dR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S1dR: Double = 0.25

    @Experimental
    @Cfg("Specify a probability to apply S2dR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S2dR: Double = 0.25

    @Experimental
    @Cfg("Specify a probability to apply SMdR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var SMdR: Double = 0.25

    @Experimental
    @Cfg("Specify a probability to enable archive-based mutation")
    @Probability
    var probOfArchiveMutation = 0.0

    @Experimental
    @Cfg("Specify a percentage (before starting a focus search) which is used by archived-based gene selection method (e.g., APPROACH_IMPACT) for selecting top percent of genes as potential candidates to mutate")
    @PercentageAsProbability(false)
    var startPerOfCandidateGenesToMutate = 0.9

    @Experimental
    @Cfg("Specify a percentage (after starting a focus search) which is used by archived-based gene selection method (e.g., APPROACH_IMPACT) for selecting top percent of genes as potential candidates to mutate")
    @PercentageAsProbability(false)
    var endPerOfCandidateGenesToMutate = 0.1

    @Experimental
    @Cfg("Specify whether to decide a top percent of genes to mutate adaptively")
    var adaptivePerOfCandidateGenesToMutate = false

    @Experimental
    @Cfg("Specify whether to enable archive-based selection for selecting genes to mutate")
    var geneSelectionMethod = GeneMutationSelectionMethod.NONE

    @Experimental
    @Cfg("Whether to enable archive-based gene mutation")
    var archiveGeneMutation = ArchiveGeneMutation.NONE

    enum class ArchiveGeneMutation {
        NONE,
        SPECIFIED,
        ADAPTIVE
    }

    @Experimental
    @Cfg("Specify whether to export derived impacts among genes")
    var exportImpacts = false

    @Experimental
    @Cfg("Specify a file that saves derived genes")
    @FilePath
    var impactFile = "impact.csv"

    @Experimental
    @Cfg("Specify whether to disable structure mutation during focus search")
    var disableStructureMutationDuringFocusSearch = false

    @Cfg("Probability to use input tracking (i.e., a simple base form of taint-analysis) to determine how inputs are used in the SUT")
    @Probability
    var baseTaintAnalysisProbability = 0.9


    @Cfg("Only used when running experiments for black-box mode, where an EvoMaster Driver would be present, and can reset state after each experiment")
    var bbExperiments = false

    @Experimental
    @Cfg("Specify whether to export covered targets info")
    var exportCoveredTarget = false

    @Experimental
    @Cfg("Specify a file which saves covered targets info regarding generated test suite")
    @FilePath
    var coveredTargetFile = "coveredTargets.txt"

    @Experimental
    @Cfg("Specify a format to organize the covered targets by the search")
    var coveredTargetSortedBy = SortCoveredTargetBy.NAME


    @FilePath
    @Cfg("When generating tests in JavaScript, there is the need to know where the driver is located in respect to" +
            " the generated tests")
    var jsControllerPath = "./app-driver.js"

    enum class SortCoveredTargetBy {
        /**
         * sorted by ids of targets alphabetically
         */
        NAME,
        /**
         * grouped by tests and sorted by index of tests.
         * it may help to analyze the individuals regarding different strategies.
         */
        TEST
        /**
         * there might be other options, e.g., based on class,
         * but we need to follow rules to encode and decode regarding id.
         */
    }


    fun timeLimitInSeconds(): Int {
        if (maxTimeInSeconds > 0) {
            return maxTimeInSeconds
        }

        val h = maxTime.indexOf('h')
        val m = maxTime.indexOf('m')
        val s = maxTime.indexOf('s')

        val hours = if (h >= 0) {
            maxTime.subSequence(0, h).toString().trim().toInt()
        } else 0

        val minutes = if (m >= 0) {
            maxTime.subSequence(if (h >= 0) h + 1 else 0, m).toString().trim().toInt()
        } else 0

        val seconds = if (s >= 0) {
            maxTime.subSequence(if (m >= 0) m + 1 else (if (h >= 0) h + 1 else 0), s).toString().trim().toInt()
        } else 0

        return (hours * 60 * 60) + (minutes * 60) + seconds
    }

}