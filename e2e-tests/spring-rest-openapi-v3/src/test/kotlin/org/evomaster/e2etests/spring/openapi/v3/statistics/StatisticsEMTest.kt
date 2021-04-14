package org.evomaster.e2etests.spring.openapi.v3.statistics

import com.foo.rest.examples.spring.openapi.v3.statistics.StatisticsController
import io.restassured.RestAssured
import org.evomaster.core.EMConfig
import org.evomaster.core.Main
import org.evomaster.core.Main.Companion.initAndRunwithStatistics
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.SearchTimeController
import org.evomaster.core.search.service.Statistics
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StatisticsEMTest :SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StatisticsController())
        }
    }

    @Test
    fun testRunEM(){
        runTestHandlingFlakyAndCompilation(
                "StatisticsEM",
                "org.foo.StatisticsEM",
                1000
        ){args: MutableList<String> ->
            args.add("--expectationsActive")
            args.add("" + true)
            args.add("--testSuiteSplitType")
            args.add("NONE")

            /*
            val injector = init(args)
            checkExperimentalSettings(injector)
            val controllerInfo = checkState(injector)

            val config = injector.getInstance(EMConfig::class.java)
            val idMapper = injector.getInstance(IdMapper::class.java)

            val writer = setupPartialOracles(injector, config)

            val solution = Main.run(injector) as Solution<RestIndividual>

            writeTests(injector, solution, controllerInfo)

            val stc = injector.getInstance(SearchTimeController::class.java)
            val statistics = injector.getInstance(Statistics::class.java)
            val data = statistics.getData(solution)
            */

            //val answer = initAndRun(args)
            val answer = initAndRunwithStatistics(args.toTypedArray())

            val solution = answer.first as Solution<RestIndividual>
            val data = answer.second

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            assertHasAtLeastOne(solution, HttpVerb.GET, 500)

            assert(data.find { p -> p.header.contains("errors5xx") }?.element == "1")
            assert(data.find { p -> p.header.contains("distinct500Faults")}?.element == "1")
            assert(data.find { p -> p.header.contains("failedOracleExpectations")}?.element == "0")
            assert(data.find { p -> p.header.contains("potentialFaults")}?.element == "2")
        }
    }
}