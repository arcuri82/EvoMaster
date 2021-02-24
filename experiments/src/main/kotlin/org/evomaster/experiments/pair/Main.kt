package org.evomaster.experiments.pair

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.Algorithm.*
import org.evomaster.core.EMConfig.FeedbackDirectedSampling.NONE
import org.evomaster.core.Lazy
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.algorithms.MosaAlgorithm
import org.evomaster.core.search.algorithms.RandomAlgorithm
import org.evomaster.core.search.algorithms.WtsAlgorithm
import org.evomaster.core.search.service.SearchAlgorithm
import java.util.*

class Main {


    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            infeasible()
        }


        private fun infeasible() {

            printHeader()

            val budget = 10_000
            val range = 10_000
            val nTargetsPerVar = 50
            val T_BOTH = "both"
            val T_ONLY_X = "onlyX"
            val types = listOf(T_BOTH, T_ONLY_X)
            val repetitions = 100 //FIXME

            for (seed in 0 until repetitions) {

                for (t in types) {

                    for (inf in listOf(1, 2, 3, 4, 5, 10, 20, 30, 40, 50)) {
//                        for (inf in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)) {

                        var optimaX: MutableList<Int>
                        var optimaY: MutableList<Int>

                        if (t == T_BOTH) {
                            val xInf = Math.ceil(inf / 2.0).toInt()
                            val yInf = Math.floor(inf / 2.0).toInt()

                            optimaX = createOptima(nTargetsPerVar - xInf, range, seed.toLong())
                                    .apply { addAll(createInfeasible(xInf)) }
                            optimaY = createOptima(nTargetsPerVar - yInf, range, seed.toLong())
                                    .apply { addAll(createInfeasible(yInf)) }
                        } else {
                            optimaX = createOptima(nTargetsPerVar - inf, range, seed.toLong())
                                    .apply { addAll(createInfeasible(inf)) }
                            optimaY = createOptima(nTargetsPerVar, range, seed.toLong())
                        }

                        Lazy.assert{optimaX.size + optimaY.size == nTargetsPerVar * 2}

                        listOf(RANDOM, MOSA, WTS).forEach { a ->
                            runAlg(a, seed.toLong(), budget, range, optimaX, optimaY, inf, NONE, t)
                        }

                        for(fds in EMConfig.FeedbackDirectedSampling.values()) {
                            runAlg(MIO, seed.toLong(), budget, range, optimaX, optimaY, inf, fds, t)
                        }
                    }
                }
            }
        }

        private fun runAlg(alg: EMConfig.Algorithm, seed: Long, budget: Int, range: Int,
                           optimaX: List<Int>, optimaY: List<Int>,
                           infeasible: Int,
                           fds: EMConfig.FeedbackDirectedSampling,
                           type: String) {

            val a = getAlg(alg, seed, budget, range, optimaX, optimaY, fds)

            val manager = a.first.getInstance(LifecycleManager::class.java)

            manager.start()
            val sol = a.second.search()
            manager.close()

            val size = optimaX.size + optimaY.size
            val covered = sol.overall.coveredTargets()
            val cov = 100.0 * (covered.toDouble() / size.toDouble())

            println("${a.second.getType()},$cov,$size,$range,$budget,$type,$infeasible,$fds")
        }

        private fun printHeader() {
            println("algorithm,coverage,n,range,budget,type,infeasible,fds")
        }


        private fun getAlg(algType: EMConfig.Algorithm,
                           seed: Long, budget: Int, range: Int,
                           optimaX: List<Int>, optimaY: List<Int>,
                           fds: EMConfig.FeedbackDirectedSampling
                           )
                : Pair<Injector, SearchAlgorithm<PairIndividual>> {

            val injector = LifecycleInjector.builder()
                    .withModules(PairModule(), BaseModule(arrayOf("--showProgress=false")))
                    .build().createInjector()

            val config = injector.getInstance(EMConfig::class.java)
//            config.showProgress = false
            config.algorithm = algType
            config.seed = seed
            config.maxActionEvaluations = budget
            config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
            config.tournamentSize = 10 //as in MOSA paper

            config.feedbackDirectedSampling = fds

            val ppd = injector.getInstance(PairProblemDefinition::class.java)
            ppd.range = range
            ppd.optimaX.clear()
            ppd.optimaX.addAll(optimaX)
            ppd.optimaY.clear()
            ppd.optimaY.addAll(optimaY)


            val alg = when (algType) {
                MIO -> injector.getInstance(Key.get(
                        object : TypeLiteral<MioAlgorithm<PairIndividual>>() {}))
                RANDOM -> injector.getInstance(Key.get(
                        object : TypeLiteral<RandomAlgorithm<PairIndividual>>() {}))
                MOSA -> injector.getInstance(Key.get(
                        object : TypeLiteral<MosaAlgorithm<PairIndividual>>() {}))
                WTS -> injector.getInstance(Key.get(
                        object : TypeLiteral<WtsAlgorithm<PairIndividual>>() {}))
            }
            return Pair(injector, alg)
        }


        private fun createInfeasible(n: Int): MutableList<Int> {
            return MutableList(n) { -1 }
        }

        private fun createOptima(n: Int, range: Int, seed: Long): MutableList<Int> {
            val optima = mutableListOf<Int>()
            val rand = Random(seed)
            (1..n).forEach {
                optima.add(rand.nextInt(range + 1))
            }

            return optima
        }
    }
}
