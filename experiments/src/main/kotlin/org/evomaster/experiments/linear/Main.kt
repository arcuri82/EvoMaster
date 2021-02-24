package org.evomaster.experiments.linear

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import com.netflix.governator.lifecycle.LifecycleManager
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.Algorithm.*
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

            base()
//            infeasible()
        }

        fun printHeader() {
            println("algorithm,coverage,n,range,budget,type,fsat,populationSize,infeasible,fds")
        }


        fun infeasible() {

            /*
                Given x (eg 10) feasible targets, study impact of adding y (eg [1, 100]) infeasible targets.
                Study MIO with and without FDS
             */
            printHeader()

            val budget = 1000
            val range = 1000
            val disruptiveP = 0.01
            val nTargets = 10
            val problemType = ProblemType.GRADIENT

            val repetitions = 100

            for (seed in 0 until repetitions) {

                val optima = createOptima(nTargets, range, seed.toLong())

                for (inf in listOf(0, 1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {

                    listOf(MIO, RANDOM, MOSA, WTS).forEach { a ->
                        runAlg(a, seed.toLong(), budget, nTargets, range, disruptiveP,
                                optima, problemType, 0.5, 50, inf, false)
                    }

                    runAlg(MIO, seed.toLong(), budget, nTargets, range, disruptiveP,
                            optima, problemType, 0.5, 50, inf, true)
                }
            }
        }

        fun tuningMIO() {

            printHeader()

            val budget = 1000
            val range = 1000
            val disruptiveP = 0.01

            val problems = listOf(ProblemType.GRADIENT, ProblemType.PLATEAU, ProblemType.DECEPTIVE)

            val repetitions = 100

            for (seed in 0 until repetitions) {
                for (nTargets in listOf(1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {

                    val optima = createOptima(nTargets, range, seed.toLong())

                    for (problemType in problems) {
                        for (fsat in listOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0)) {

                            runAlg(MIO, seed.toLong(), budget, nTargets, range, disruptiveP,
                                    optima, problemType, fsat, 50, 0, true)
                        }
                    }
                }
            }
        }

        fun tuningMOSA() {

            printHeader()

            val budget = 1000
            val range = 1000
            val disruptiveP = 0.01

            val problems = listOf(ProblemType.GRADIENT, ProblemType.PLATEAU, ProblemType.DECEPTIVE)

            val repetitions = 100

            for (seed in 0 until repetitions) {
                for (nTargets in listOf(1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {

                    val optima = createOptima(nTargets, range, seed.toLong())

                    for (problemType in problems) {
                        for (pSize in listOf(4, 8, 16, 32, 64, 128)) {

                            runAlg(MOSA, seed.toLong(), budget, nTargets, range, disruptiveP,
                                    optima, problemType, 0.5, pSize, 0, false)
                        }
                    }
                }
            }
        }

        fun base() {

            printHeader()

            val budget = 1000
            val range = 1000
            val disruptiveP = 0.01
            val problemType = ProblemType.GRADIENT
//            val problemType = ProblemType.PLATEAU
//            val problemType = ProblemType.DECEPTIVE

            val repetitions = 100

            for (seed in 0 until repetitions) {
                for (nTargets in listOf(1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {

                    val optima = createOptima(nTargets, range, seed.toLong())


                    runBase(seed.toLong(), budget, nTargets, range, disruptiveP, optima, problemType, 0.5, 50)
                }
            }
        }

        fun runAlg(alg: EMConfig.Algorithm, seed: Long, budget: Int, nTargets: Int,
                   range: Int, disruptiveP: Double, optima: List<Int>, problemType: ProblemType,
                   fsat: Double, populationSize: Int,
                   infeasible: Int, fds: Boolean) {

            val a = getAlg(alg, seed, budget, nTargets, range, disruptiveP,
                    optima, problemType, fsat, populationSize, infeasible, fds)

            val manager = a.first.getInstance(LifecycleManager::class.java)

            manager.start()
            val sol = a.second.search()
            manager.close()

            val covered = sol.overall.coveredTargets()
            val cov = 100.0 * (covered.toDouble() / nTargets.toDouble())

            println("${a.second.getType()},$cov,$nTargets,$range,$budget,$problemType,$fsat,$populationSize" +
                    ",$infeasible,$fds")
        }

        fun runBase(seed: Long, budget: Int, nTargets: Int, range: Int, disruptiveP: Double,
                    optima: List<Int>, problemType: ProblemType, fsat: Double, populationSize: Int) {

            listOf(MIO, RANDOM, MOSA, WTS).forEach { a ->
                runAlg(a, seed, budget, nTargets, range, disruptiveP,
                        optima, problemType, fsat, populationSize, 0, true)
            }
        }


        fun createOptima(n: Int, range: Int, seed: Long): List<Int> {
            val optima = mutableListOf<Int>()
            val rand = Random(seed)
            (1..n).forEach {
                optima.add(rand.nextInt(range + 1))
            }

            return optima
        }

        fun getAlg(algType: EMConfig.Algorithm, seed: Long, budget: Int, nTargets: Int,
                   range: Int, disruptiveP: Double, optima: List<Int>, problemType: ProblemType,
                   fsat: Double, populationSize: Int,
                   infeasible: Int, fds: Boolean)
                : Pair<Injector, SearchAlgorithm<LinearIndividual>> {

            val injector = LifecycleInjector.builder()
                    .withModules(* arrayOf<Module>(LinearModule(), BaseModule()))
                    .build().createInjector()

            val config = injector.getInstance(EMConfig::class.java)
            config.algorithm = algType
            config.seed = seed
            config.maxActionEvaluations = budget
            config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
            config.tournamentSize = 10 //as in MOSA paper
            config.focusedSearchActivationTime = fsat
            config.populationSize = populationSize

            config.feedbackDirectedSampling = if (fds)
                EMConfig.FeedbackDirectedSampling.LAST
            else EMConfig.FeedbackDirectedSampling.NONE

            val lpd = injector.getInstance(LinearProblemDefinition::class.java)
            lpd.nTargets = nTargets
            lpd.disruptiveP = disruptiveP
            lpd.range = range
            lpd.problemType = problemType
            lpd.infeasible = infeasible
            lpd.optima.clear()
            lpd.optima.addAll(optima)


            val alg = when (algType) {
                MIO -> injector.getInstance(Key.get(
                        object : TypeLiteral<MioAlgorithm<LinearIndividual>>() {}))
                RANDOM -> injector.getInstance(Key.get(
                        object : TypeLiteral<RandomAlgorithm<LinearIndividual>>() {}))
                MOSA -> injector.getInstance(Key.get(
                        object : TypeLiteral<MosaAlgorithm<LinearIndividual>>() {}))
                WTS -> injector.getInstance(Key.get(
                        object : TypeLiteral<WtsAlgorithm<LinearIndividual>>() {}))
            }
            return Pair(injector, alg)
        }


    }
}
