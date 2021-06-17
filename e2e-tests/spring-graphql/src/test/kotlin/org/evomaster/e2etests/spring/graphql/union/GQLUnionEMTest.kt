package org.evomaster.e2etests.spring.graphql.union


import com.foo.graphql.union.UnionController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLUnionEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(UnionController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_UnionEM",
                "org.foo.graphql.UnionEM",
                20
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertNoneWithErrors(solution)
        }
    }
}