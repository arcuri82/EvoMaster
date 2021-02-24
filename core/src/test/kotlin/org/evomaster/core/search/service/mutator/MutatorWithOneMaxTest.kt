package org.evomaster.core.search.service.mutator

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.onemax.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Paths

class MutatorWithOneMaxTest {

    private lateinit var injector: Injector
    private lateinit var config : EMConfig
    private lateinit var manager : LifecycleManager

    @BeforeEach
    fun init(){

        injector = LifecycleInjector.builder()
                .withModules(ManipulatedOneMaxModule(), BaseModule())
                .build().createInjector()

        manager = injector.getInstance(LifecycleManager::class.java)
        config = injector.getInstance(EMConfig::class.java)
        config.useTimeInFeedbackSampling = false // non-deterministic
    }

    private fun reportFP(path : String, improve : Boolean) : Double{
        val contents = Files.readAllLines(Paths.get(path))

        val tp = contents.count {
            val result = it.split(",")[1]
            if (improve)
                result == EvaluatedMutation.WORSE_THAN.name
            else
                result == EvaluatedMutation.BETTER_THAN.name
        }

        return tp * 1.0 / contents.size
    }

    private fun setAndRun(n : Int, improve: Boolean, strategy : EMConfig.MutationTargetsSelectionStrategy, budget : Int) : Double{
        injector.getInstance(ManipulatedOneMaxMutator::class.java).improve = improve
        injector.getInstance(OneMaxSampler::class.java).n = n
        config.mutationTargetsSelectionStrategy = strategy

        config.maxActionEvaluations = budget
        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS

        config.saveMutationInfo = true
        config.mutatedGeneFile = "target/MutatorWithOneMaxTest/targets${n}And${improve}ImproveMutationAnd${strategy}First.csv"
        Files.deleteIfExists(Paths.get(config.mutatedGeneFile))

        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        manager.start()
        mio.search()
        manager.close()

        return reportFP(config.mutatedGeneFile, improve)
    }
    // 50 targets with worsening mutation (F) with FIRST_NOT_COVERED_TARGET
    @Test
    fun testWith50FFirst(){
        val n = 50
        val improve = false
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 50 targets with worsening mutation (F) with EXPANDED_UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith50FExpand(){
        val n = 50
        val improve = false
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 50 targets with worsening mutation (F) with UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith50FUpdate(){
        val n = 50
        val improve = false
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }


    // 50 targets with improving mutation (T) with FIRST_NOT_COVERED_TARGET
    @Test
    fun testWith50TFirst(){
        val n = 50
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 50 targets with improving mutation (T) with EXPANDED_UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith50TExpand(){
        val n = 50
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 50 targets with improving mutation (T) with UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith50TUpdate(){
        val n = 50
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 100 targets with improving mutation (T) with FIRST_NOT_COVERED_TARGET
    @Test
    fun testWith100TFirst(){
        val n = 100
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 100 targets with improving mutation (T) with EXPANDED_UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith100TExpand(){
        val n = 100
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 100 targets with improving mutation (T) with UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith100TUpdate(){
        val n = 100
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 500 targets with improving mutation (T) with FIRST_NOT_COVERED_TARGET
    @Test
    fun testWith500TFirst(){
        val n = 500
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 500 targets with improving mutation (T) with EXPANDED_UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith500TExpand(){
        val n = 500
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }

    // 500 targets with improving mutation (T) with UPDATED_NOT_COVERED_TARGET
    @Test
    fun testWith500TUpdate(){
        val n = 500
        val improve = true
        val result = setAndRun(n, improve, EMConfig.MutationTargetsSelectionStrategy.UPDATED_NOT_COVERED_TARGET, 100)

        assertTrue(result < 0.1, "less than 0.1 is expected, but $result")
    }
}