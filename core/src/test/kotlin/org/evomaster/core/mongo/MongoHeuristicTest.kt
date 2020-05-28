package org.evomaster.core.mongo

import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.Param
import org.junit.jupiter.api.Test

class MongoHeuristicTest {

    @Test
    fun testIndividual() {
        val sampleType = SampleType.RANDOM

        val restActions = emptyList<RestAction>().toMutableList()

        val restAction = RestCallAction(id = "GET/api/foo", verb = HttpVerb.GET, path = RestPath("/api/foo"), parameters = emptyList<Param>().toMutableList())
        restActions.add(restAction)

        val individual = RestIndividual(restActions, sampleType)

        checkNotNull(individual)
    }
}