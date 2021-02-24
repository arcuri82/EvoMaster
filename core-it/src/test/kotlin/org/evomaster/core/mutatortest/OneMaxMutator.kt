package org.evomaster.core.mutatortest

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.onemax.ManipulatedOneMaxModule
import org.evomaster.core.search.algorithms.onemax.ManipulatedOneMaxMutator
import org.evomaster.core.search.algorithms.onemax.OneMaxIndividual
import org.evomaster.core.search.algorithms.onemax.OneMaxSampler
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

/**
 * created by manzh on 2020-06-24
 */
class OneMaxMutator {

    private val utest = MannWhitneyUTest()

    fun mutatorExp(){
        val targetsExp = listOf(50, 100, 500, 1000)
        val first = listOf(true, false)
        val improve = listOf(true, false)

        val sample = 30

        val path = Paths.get("target/mutatorWithOneMaxTest/summary.txt")
        if (Files.exists(path))
            Files.write(path, LocalDateTime.now().toString().toByteArray(), StandardOpenOption.APPEND)
        else{
            Files.createDirectories(path.parent)
            Files.write(path, LocalDateTime.now().toString().toByteArray())
        }


        listOf(100, 500, 900).forEach {b->
            targetsExp.forEach { n->
                improve.forEach { i->
                    val map = mutableMapOf<String, MutableList<Pair<Double, Double>>>()
                    val content = mutableListOf<String>()
                    first.forEach { f->
                        (0 until sample).forEach {runs->
                            run(runs, n, i, f, b, map)
                        }
                    }

                    map.forEach { t, u ->
                        content.add("$t ,tp: ${u.map { it.first }.average()}, coverage: ${u.map { it.second }.average()}")
                    }
                    val keys = map.keys.toList()
                    assert(map.size == 2)

                    val pTP = utest.mannWhitneyUTest(map[keys.first()]!!.map { it.first }.toDoubleArray(), map[keys.last()]!!.map { it.first }.toDoubleArray())
                    val pCOV = utest.mannWhitneyUTest(map[keys.first()]!!.map { it.second }.toDoubleArray(), map[keys.last()]!!.map { it.second }.toDoubleArray())
                    content.add("${keys.first()} vs. ${keys.last()}, tp:$pTP, coverage: $pCOV")
                    Files.write(path, content, StandardOpenOption.APPEND)
                }

            }
        }
    }


    private fun run(runs : Int, n : Int, improve: Boolean, first : Boolean, budget : Int, map: MutableMap<String, MutableList<Pair<Double, Double>>>){

        val injector: Injector = LifecycleInjector.builder()
                .withModules(ManipulatedOneMaxModule(), BaseModule())
                .build().createInjector()

        val  mutator = injector.getInstance(Key.get(
                object : TypeLiteral<ManipulatedOneMaxMutator>() {}))

        val mio = injector.getInstance(Key.get(
                object : TypeLiteral<MioAlgorithm<OneMaxIndividual>>() {}))

        val config = injector.getInstance(EMConfig::class.java)

        val sampler = injector.getInstance(OneMaxSampler::class.java)

        config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
        mutator.improve = improve
        sampler.n = n
        config.showProgress = false
        config.mutationTargetsSelectionStrategy = if (first) EMConfig.MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET else EMConfig.MutationTargetsSelectionStrategy.EXPANDED_UPDATED_NOT_COVERED_TARGET
        config.maxActionEvaluations = budget

        val expId = listOf("${config.mutationTargetsSelectionStrategy}", "budget-$budget","#targets-$n", "improvingMutator-$improve").joinToString("_")

        config.mutatedGeneFile = "target/mutatorWithOneMaxTest/${expId}_runs$runs.csv"
        Files.deleteIfExists(Paths.get(config.mutatedGeneFile))


        val m = injector.getInstance(LifecycleManager::class.java)
        m.start()
        val solution = mio.search()
        m.close()

        val tp = reportFP(config.mutatedGeneFile, improve)
        val covered = solution.overall.coveredTargets() * 1.0 /n

        val result = Pair(tp, covered)
        map.getOrPut(expId){ mutableListOf()}.add(result)
    }

    private fun reportFP(path : String, improve : Boolean) : Double{
        val contents = Files.readAllLines(Paths.get(path))

        val tp = contents.count {
            val result = it.split(",")[1]
            if (improve) result == EvaluatedMutation.WORSE_THAN.name else result != EvaluatedMutation.WORSE_THAN.name
        }

        return tp * 1.0 / contents.size
    }
}

fun main(array: Array<String>){
    OneMaxMutator().mutatorExp()
}