package org.evomaster.core.output.service

import com.google.inject.Inject
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.core.EMConfig
import org.evomaster.core.output.*
import org.evomaster.core.problem.rest.BlackBoxUtils
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchTimeController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime


/**
 * Given a Solution as input, convert it to a string representation of
 * the tests that can be written to file and be compiled
 */
class TestSuiteWriter {

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var searchTimeController: SearchTimeController

    private lateinit var swagger: OpenAPI
    private lateinit var partialOracles: PartialOracles
    private lateinit var objectGenerator: ObjectGenerator

    private var active = mutableMapOf<String, Boolean>()

    companion object {
        const val jsImport = "EM";

        private const val controller = "controller"
        private const val baseUrlOfSut = "baseUrlOfSut"
        private const val expectationsMasterSwitch = "ems"
        private const val fixtureClass = "ControllerFixture"
        private const val fixture = "fixture"

        private val testCaseWriter = TestCaseWriter()

        private val log: Logger = LoggerFactory.getLogger(TestSuiteWriter::class.java)
    }

    fun setSwagger(sw: OpenAPI) {
        swagger = sw
    }

    fun writeTests(
            solution: Solution<*>,
            controllerName: String?,
            timestamp : String = ""
    ) {

        if(!::partialOracles.isInitialized) partialOracles = PartialOracles()

        val name = TestSuiteFileName(calculateClassName(solution))

        val content = convertToCompilableTestCode(solution, name, timestamp, controllerName)
        saveToDisk(content, config, name)
    }

    private fun calculateClassName(solution: Solution<*>): String {
        return "${solution.testSuiteName}${solution.termination.suffix}"
    }

    private fun isAnSnapshot(solution: Solution<*>) =
            Termination.IN_PROGRESS == solution.termination


    fun convertToCompilableTestCode(
            solution: Solution<*>,
            testSuiteFileName: TestSuiteFileName,
            timestamp : String = "",
            controllerName: String?
    )
            : String {

        val lines = Lines()
        val testSuiteOrganizer = TestSuiteOrganizer()
        partialOracles.setFormat(config.outputFormat)
        if (::swagger.isInitialized) testCaseWriter.setSwagger(swagger)
        testCaseWriter.setPartialOracles(partialOracles)
        active = partialOracles.activeOracles(solution.individuals as MutableList<EvaluatedIndividual<RestIndividual>>)

        header(solution, testSuiteFileName, lines, timestamp, controllerName)

        if (config.outputFormat.isJavaOrKotlin()) {
            /*
                In Java/Kotlin the tests are inside a class, but not in JS
             */
            lines.indent()
        }

        beforeAfterMethods(controllerName, lines, config.outputFormat, testSuiteFileName)

        //catch any sorting problems (see NPE is SortingHelper on Trello)
        val tests = try {
            testSuiteOrganizer.sortTests(solution, config.customNaming)
        } catch (ex: Exception) {
            var counter = 0
            log.warn("A failure has occurred with the test sorting. Reverting to default settings. \n"
                    + "Exception: ${ex.localizedMessage} \n"
                    + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. ")
            solution.individuals.map { ind -> TestCase(ind, "test_${counter++}") }
        }

        for (test in tests) {
            lines.addEmpty(2)

            // catch writing problems on an individual test case basis
            val testLines = try {
                testCaseWriter.convertToCompilableTestCode(config, test, baseUrlOfSut)
            } catch (ex: Exception) {
                log.warn("A failure has occurred in writing test ${test.name}. \n "
                        + "Exception: ${ex.localizedMessage} \n"
                        + "At ${ex.stackTrace.joinToString(separator = " \n -> ")}. ")
                Lines()
            }
            lines.add(testLines)
        }

        if(config.outputFormat.isJavaOrKotlin()){
            lines.deindent()
        }

        footer(lines)

        return lines.toString()
    }


    private fun saveToDisk(testFileContent: String,
                           config: EMConfig,
                           testSuiteFileName: TestSuiteFileName) {

        val path = Paths.get(config.outputFolder, testSuiteFileName.getAsPath(config.outputFormat))

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }

    private fun classDescriptionEmptyLine(lines: Lines) {
        if (config.outputFormat.isJava()) {
            lines.add(" * <br>")
        } else {
            lines.add(" * ")
        }
    }

    private fun escapeDocs(s: String): String {
        return if (config.outputFormat.isKotlin()) {
            //in Kotlin Docs, [] has special meaning
            s.replace("[", "\\[").replace("]", "\\]")
        } else {
            s
        }
    }

    private fun classDescriptionComment(solution: Solution<*>, lines: Lines, timestamp : String = "") {
        lines.add("/**")
        lines.add(" * This file was automatically generated by EvoMaster on ${escapeDocs(ZonedDateTime.now().toString())}")
        classDescriptionEmptyLine(lines)
        if (isAnSnapshot(solution)) {
            lines.add(" ************************************ WARNING ************************************ ")
            lines.add(" * This is an snapshot of the generated tests after $timestamp seconds elapsed. *")
            lines.add(" * The execution of Evomaster have not finished. *")
            lines.add(" ********************************************************************************* ")
        }
        lines.add(" * The generated test suite contains ${solution.individuals.size} tests")
        classDescriptionEmptyLine(lines)
        lines.add(" * Covered targets: ${solution.overall.coveredTargets()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * Used time: ${searchTimeController.getElapsedTime()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * Needed budget for current results: ${searchTimeController.neededBudget()}")
        classDescriptionEmptyLine(lines)
        lines.add(" * ${solution.termination.comment}")
        lines.add(" */")

    }

    private fun defineFixture(lines: Lines, controllerName: String?){
        lines.add("public class $fixtureClass : IDisposable")

        lines.block {
            lines.addEmpty(2)
            lines.add("public ISutHandler $controller { get; private set; }")
            lines.add("public string $baseUrlOfSut { get; private set; }")

            lines.addEmpty()

            lines.add("public $fixtureClass()")
            lines.block {

                lines.addEmpty(1)
                addStatement("$controller = new $controllerName()", lines)
                addStatement("$controller.SetupForGeneratedTest()", lines)
                addStatement("$baseUrlOfSut = $controller.StartSut ()", lines)
                addStatement("Assert.NotNull($baseUrlOfSut)", lines)

            }

            lines.addEmpty()

            lines.add("public void Dispose()")
            lines.block {
                addStatement("$controller.StopSut ()", lines)
            }
        }
        lines.addEmpty()
    }

    private fun header(solution: Solution<*>,
                       name: TestSuiteFileName,
                       lines: Lines,
                       timestamp : String = "",
                       controllerName: String?) {

        val format = config.outputFormat

        if (name.hasPackage() && format.isJavaOrKotlin()) {
            addStatement("package ${name.getPackage()}", lines)
            lines.addEmpty(2)
        }

        if (format.isJUnit5()) {
            addImport("org.junit.jupiter.api.AfterAll", lines)
            addImport("org.junit.jupiter.api.BeforeAll", lines)
            addImport("org.junit.jupiter.api.BeforeEach", lines)
            addImport("org.junit.jupiter.api.Test", lines)
            addImport("org.junit.jupiter.api.Assertions.*", lines, true)
        }
        if (format.isJUnit4()) {
            addImport("org.junit.AfterClass", lines)
            addImport("org.junit.BeforeClass", lines)
            addImport("org.junit.Before", lines)
            addImport("org.junit.Test", lines)
            addImport("org.junit.Assert.*", lines, true)
        }

        if (format.isJava()) {
            //in Kotlin this should not be imported
            addImport("java.util.Map", lines)
        }

        if (format.isJavaOrKotlin()) {
            addImport("io.restassured.RestAssured", lines)
            addImport("io.restassured.RestAssured.given", lines, true)
            addImport("io.restassured.response.ValidatableResponse", lines)
            addImport("org.evomaster.client.java.controller.api.EMTestUtils.*", lines, true)
            addImport("org.evomaster.client.java.controller.SutHandler", lines)
            addImport("org.evomaster.client.java.controller.db.dsl.SqlDsl.sql", lines, true)
            addImport(InsertionDto::class.qualifiedName!!, lines)
            addImport("java.util.List", lines)


            // TODO: BMR - this is temporarily added as WiP. Should we have a more targeted import (i.e. not import everything?)
            if (config.enableBasicAssertions) {
                addImport("org.hamcrest.Matchers.*", lines, true)
                //addImport("org.hamcrest.core.AnyOf.anyOf", lines, true)
                addImport("io.restassured.config.JsonConfig", lines)
                addImport("io.restassured.path.json.config.JsonPathConfig", lines)
                addImport("io.restassured.config.RedirectConfig.redirectConfig", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.NumberMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.StringMatcher.*", lines, true)
                addImport("org.evomaster.client.java.controller.contentMatchers.SubStringMatcher.*", lines, true)
            }

            if (config.expectationsActive) {
                addImport("org.evomaster.client.java.controller.expect.ExpectationHandler.expectationHandler", lines, true)
                addImport("org.evomaster.client.java.controller.expect.ExpectationHandler", lines)
                addImport("io.restassured.path.json.JsonPath", lines)
                addImport("java.util.Arrays", lines)
            }
        }

        if (format.isJavaScript()) {
            lines.add("const superagent = require(\"superagent\");")
            lines.add("const $jsImport = require(\"evomaster-client-js\");")
            if(controllerName != null) {
                lines.add("const $controllerName = require(\"${config.jsControllerPath}\");")
            }
        }

        if(format.isCsharp()){
            addUsing("Xunit", lines)
            addUsing("System.Net.Http", lines)
            addUsing("System.Threading.Tasks", lines)

        }

        lines.addEmpty(4)

        classDescriptionComment(solution, lines, timestamp)

        if(format.isCsharp()){
            defineFixture(lines, controllerName)
        }
        if (format.isJavaOrKotlin() || format.isCsharp()) {
            defineClass(name, lines)
            lines.addEmpty()
        }
    }

    private fun staticVariables(controllerName: String?, lines: Lines) {

        if (config.outputFormat.isJava()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private static final SutHandler $controller = new $controllerName();")
                lines.add("private static String $baseUrlOfSut;")
            } else {
                lines.add("private static String $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\";")
            }
        } else if (config.outputFormat.isKotlin()) {
            if (!config.blackBox || config.bbExperiments) {
                lines.add("private val $controller : SutHandler = $controllerName()")
                lines.add("private lateinit var $baseUrlOfSut: String")
            } else {
                lines.add("private val $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\"")
            }
        } else if (config.outputFormat.isJavaScript()) {

            if (!config.blackBox || config.bbExperiments) {
                lines.add("const $controller = new $controllerName();")
                lines.add("let $baseUrlOfSut;")
            } else {
                lines.add("const $baseUrlOfSut = \"${BlackBoxUtils.restUrl(config)}\";")
            }
        }
        else if(config.outputFormat.isCsharp()){
            if (!config.blackBox || config.bbExperiments){
                lines.add("private static readonly HttpClient client = new HttpClient ();")
            }
        }

        if(config.expectationsActive) {
            if (config.outputFormat.isJavaOrKotlin()) {
                //TODO JS
                if(active.any{it.value}) {
                    lines.add("/** [$expectationsMasterSwitch] - expectations master switch - is the variable that activates/deactivates expectations " +
                            "individual test cases")
                    lines.add(("* by default, expectations are turned off. The variable needs to be set to [true] to enable expectations"))
                    lines.add("*/")
                    if(config.outputFormat.isJava()){
                        lines.add("private static boolean $expectationsMasterSwitch = false;")
                    }
                    else if(config.outputFormat.isKotlin()){
                        lines.add("private val $expectationsMasterSwitch = false")
                    }
                }
                partialOracles.variableDeclaration(lines, config.outputFormat, active)
            }
        }
        //Note: ${config.expectationsActive} can be used to get the active setting, but the default
        // for generated code should be false.
    }

    private fun initClassMethod(lines: Lines) {

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@BeforeClass")
            format.isJUnit5() -> lines.add("@BeforeAll")
        }
        when {
            format.isJava() -> lines.add("public static void initClass()")
            format.isKotlin() -> {
                lines.add("@JvmStatic")
                lines.add("fun initClass()")
            }
            format.isJavaScript() -> lines.add("beforeAll( async () =>");
        }

        lines.block {
            if (!config.blackBox) {
                when {
                    config.outputFormat.isJavaScript() -> {
                        addStatement("await $controller.setupForGeneratedTest()", lines)
                        addStatement("baseUrlOfSut = await $controller.startSut()", lines)
                    }
                    config.outputFormat.isJavaOrKotlin() -> {
                        addStatement("$controller.setupForGeneratedTest()", lines)
                        addStatement("baseUrlOfSut = $controller.startSut()", lines)
                    }
                }

                when {
                    format.isJavaOrKotlin() -> addStatement("assertNotNull(baseUrlOfSut)", lines)
                    format.isJavaScript() -> addStatement("expect(baseUrlOfSut).toBeTruthy()", lines)
                }
            }

            if (format.isJavaOrKotlin()) {
                addStatement("RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()", lines)
                addStatement("RestAssured.useRelaxedHTTPSValidation()", lines)
                addStatement("RestAssured.urlEncodingEnabled = false", lines)
            }

            if (config.enableBasicAssertions && format.isJavaOrKotlin()) {
                lines.add("RestAssured.config = RestAssured.config()")
                lines.indented {
                    lines.add(".jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))")
                    lines.add(".redirect(redirectConfig().followRedirects(false))")
                }
                appendSemicolon(lines)
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun tearDownMethod(lines: Lines) {

        if (config.blackBox) {
            return
        }

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@AfterClass")
            format.isJUnit5() -> lines.add("@AfterAll")
        }
        when {
            format.isJava() -> lines.add("public static void tearDown()")
            format.isKotlin() -> {
                lines.add("@JvmStatic")
                lines.add("fun tearDown()")
            }
            format.isJavaScript() -> lines.add("afterAll( async () =>")
        }

        if(!format.isCsharp()){
            lines.block {
                when {
                    format.isJavaScript() -> {
                        addStatement("await $controller.stopSut()", lines)
                    }
                    else -> {
                        addStatement("$controller.stopSut()", lines)
                    }
                }
            }
        }

        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun initTestMethod(lines: Lines, name: TestSuiteFileName) {

        if (config.blackBox) {
            return
        }

        val format = config.outputFormat

        when {
            format.isJUnit4() -> lines.add("@Before")
            format.isJUnit5() -> lines.add("@BeforeEach")
        }
        when {
            format.isJava() -> lines.add("public void initTest()")
            format.isKotlin() -> {
                lines.add("fun initTest()")
            }
            format.isJavaScript() -> lines.add("beforeEach(async () => ");
            format.isCsharp() -> lines.add("public ${name.getClassName()} ()")
        }


            lines.block {
                if(format.isJavaScript()){
                    addStatement("await $controller.resetStateOfSUT()", lines)
                } else if(format.isJavaOrKotlin()) {
                    addStatement("$controller.resetStateOfSUT()", lines)
                }
                else if(format.isCsharp()){
                    addStatement("$controller.ResetStateOfSUT()", lines)
                }
            }


        if (format.isJavaScript()) {
            lines.append(");")
        }
    }

    private fun beforeAfterMethods(controllerName: String?, lines: Lines, format: OutputFormat, testSuiteFileName: TestSuiteFileName) {

        lines.addEmpty()

        val staticInit = {
            staticVariables(controllerName, lines)

            if(!format.isCsharp()){
                lines.addEmpty(2)
                initClassMethod(lines)
                lines.addEmpty(2)

                tearDownMethod(lines)
            }
            else{
                addStatement("$fixtureClass $fixture",lines)
            }
        }

        if (config.outputFormat.isKotlin()) {
            lines.add("companion object")
            lines.block(1, staticInit)
        } else {
            staticInit.invoke()
        }
        lines.addEmpty(2)

        initTestMethod(lines, testSuiteFileName)
        lines.addEmpty(2)
    }


    private fun footer(lines: Lines) {
        if (config.outputFormat.isJavaOrKotlin()) {
            lines.addEmpty(2)
            lines.add("}")
        }
    }

    private fun defineClass(name: TestSuiteFileName, lines: Lines) {

        lines.addEmpty()

        val format = config.outputFormat

        when {
            format.isJava() -> lines.append("public ")
            format.isKotlin() -> lines.append("internal ")
            format.isCsharp() -> lines.append("public ")
        }

        if(!format.isCsharp())
            lines.append("class ${name.getClassName()} {")
        else
            lines.append("class ${name.getClassName()} : IClassFixture<$fixtureClass> {")
    }

    private fun addImport(klass: String, lines: Lines, static: Boolean = false) {

        //Kotlin for example does not use "static" in the imports
        val s = if (static && config.outputFormat.isJava()) "static" else ""

        addStatement("import $s $klass", lines)
    }

    private fun addUsing(library:String, lines: Lines, static: Boolean = false){

        val s = if(static) "static" else ""

        addStatement("using $s $library", lines)
    }

    private fun addStatement(statement: String, lines: Lines) {
        lines.add(statement)
        appendSemicolon(lines)
    }

    private fun appendSemicolon(lines: Lines) {
        if (config.outputFormat.let { it.isJava() || it.isJavaScript() || it.isCsharp() }) {
            lines.append(";")
        }
    }

    fun setPartialOracles(oracles: PartialOracles){
        partialOracles = oracles
    }
    fun getPartialOracles(): PartialOracles{
        return partialOracles
    }
    fun setObjectGenerator(generator: ObjectGenerator){
        objectGenerator = generator
    }
}
