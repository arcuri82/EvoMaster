package org.evomaster.experiments.objects

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Solution
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.experiments.objects.service.ObjModule

class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

           // Main.testMapPrint()
            Main.base()
//            infeasible()
        }

        private fun base () {
            printHeader()

            val injector = setStuffUp()
            //val sampler = injector.getInstance(ObjRestSampler::class.java)

            val key = Key.get( object : TypeLiteral<MioAlgorithm<ObjIndividual>>() {})

            val imp = injector.getInstance(key)

            //BMR: debugginPrint uses .getName() which returns {variable} etc...
            //BMR: debugginPrintProcessed uses .toString(), and seems to return the {variable} values already processed.


            /*
            val models = sampler.getModelCluster()
                    .filter { (_, m) -> !(m.fields.isEmpty()) } as MutableMap
            // TODO: this needs to be thought out better in future, but let's trim the model set to remove empties.


            if (models.isEmpty()){
                val col = mutableListOf(StringGene("default"))
                val someCol = ObjectGene(
                        "Default",
                        col
                )
                models["Default"] = someCol
            }

            val available = sampler.seeAvailableActions()

            println("Overview of Models:")
            models.forEach{
                println("${it.key} => ${it.value.getValueAsPrintableString()}")
            }
            */

            //nameMatchExperiments(models)



            println("===============================================>")

            /*
            println("Available callActions:")
            available.forEach{it ->
                println("===============================================>")

                val action = sampler.sampleRandomObjCallAction(0.0)
                println(action.getName())

                println("Action after selection: ${action.resolvedPath()}")

            }*/

            // TODO: when picking an object, make sure it has fields to match the data required by the action.
            // E.g. do not pick an object without any numerical values, if an Int id is required.

            println("===============================================>")

            //randomObjectExperiments(sampler, 10)


            println("===============================================>")
            //swaggerishObjectExperiments(sampler, 50)

            //val smarts = sampler.smartSample()
            //println("One more try on smart: ${smarts.debugginPrint()} => ${smarts.debugginPrintProcessed()}")
            //var smarts = sampler.getRandomObjIndividual()
            //println("Let's try to be smart again: ${smarts.debugginPrintProcessed()}")
            // TODO: Smart needs more work

            val config = injector.getInstance(EMConfig::class.java)
            val rc = injector.getInstance(RemoteController::class.java)
            val controllerInfoDto = rc.getControllerInfo() ?:
            throw IllegalStateException(
                    "Cannot retrieve Remote Controller info from ${rc.host}:${rc.port}")

            config.stoppingCriterion = EMConfig.StoppingCriterion.FITNESS_EVALUATIONS
            config.maxActionEvaluations = 1000
            /*
            TODO: dbInit ?
            BMR: Writing tests appears to work, but dbInit has been removed (some gene problems emerging from the
            current work being in a different folder. This should be reinstated later.
            */

            config.outputFolder = "experiments/bmr/test"

            println("===============================================>")
            println("The search:")

            val solution = imp.search()
            println("Solution: ${solution}:")
            for (individual in solution.individuals) {
                println(" --- ")
                println("${individual.individual.debugginPrint()} => ${individual.individual.debugginPrintProcessed()} => ${individual.fitness.computeFitnessScore()}")
                println("Valid? Well... ${individual.individual.checkCoherence()}")
            }



            /*ObjTestSuiteWriter.writeTests(
                    solution,
                    controllerInfoDto.fullName,
                    config
            )*/


        }

        fun printHeader() {
            println("Let's start playing about")
        }

        fun setStuffUp () : Injector{

            val base = BaseModule()
            //BMR: just to get it running (and to have a clue how to add the selector to it
            val problemType = EMConfig.ProblemType.REST

            val problemModule = when (problemType) {
                EMConfig.ProblemType.REST -> ObjModule()
                //this should never happen, unless we add new type and forget to add it here
                else -> throw IllegalStateException("Unrecognized problem type: $problemType")
            }


            val injector = LifecycleInjector.builder()
                    .withModules(base, problemModule)
                    .build()
                    .createInjector()

            return injector

        }



    }

}
