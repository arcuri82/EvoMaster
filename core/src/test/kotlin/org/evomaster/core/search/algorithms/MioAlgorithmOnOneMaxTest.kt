package org.evomaster.core.search.algorithms

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxModule
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class MioAlgorithmOnOneMaxTest {

    val injector: Injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(OneMaxModule(), BaseModule()))
                    .build().createInjector()

    @Test
    fun testMIO(){

        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        val randomness = injector.getInstance(Randomness::class.java)
        randomness.updateSeed(42)

        val sampler = injector.getInstance(OneMaxSampler::class.java)

        val config = injector.getInstance(EMConfig::class.java)
        config.maxActionEvaluations = 30000
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        val n = 20
        sampler.n = n

        val solution = mio.search()

        Assertions.assertEquals(n.toDouble(), solution.overall.computeFitnessScore(), 0.001);
        Assertions.assertEquals(1, solution.individuals.size)
    }
}
