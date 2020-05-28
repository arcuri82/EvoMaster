package org.evomaster.core.search

import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.SecondaryObjectiveStrategy.*
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.mongo.MongoExecution
import org.evomaster.core.search.service.IdMapper
import kotlin.math.min

/**
As the number of targets is unknown, we cannot have
a minimization problem, as new targets could be added
throughout the search
 */
class FitnessValue(
        /** An estimation of the size of the individual that obtained
         * this fitness value. Longer individuals are worse, but only
         * when fitness is not strictly better */
        var size: Double) {

    init {
        if (size < 0.0) {
            throw IllegalArgumentException("Invalid size value: $size")
        }
    }

    companion object {

        const val MAX_VALUE = 1.0

        fun isMaxValue(value: Double) = value == MAX_VALUE
    }

    /**
     *  Key -> target Id
     *
     *  Value -> heuristic distance in [0,1], where 1 is for "covered"
     */
    private val targets: MutableMap<Int, Heuristics> = mutableMapOf()

    /**
     *  Key -> action Id
     *
     * Value -> List of extra heuristics to minimize (min 0).
     * Those are related to the whole test, and not specific target.
     * Covering those extra does not guarantee that it would help in
     * covering target.
     * An example is rewarding SQL Select commands that return non-empty
     *
     * Note: these values are SORTED.
     */
    private val extraToMinimize: MutableMap<Int, List<Double>> = mutableMapOf()


    /**
     * Key -> action Id
     *
     * Value -> info on how the SQL database was accessed
     */
    private val databaseExecutions: MutableMap<Int, DatabaseExecution> = mutableMapOf()


    /**
     * Key -> action Id
     *
     * Value -> info on how the Mongo database was accessed during that action
     */
    private val mongoExecutions: MutableMap<Int, MongoExecution> = mutableMapOf()


    /**
     * When SUT does SQL commands using WHERE, keep track of when those "fails" (ie evaluate
     * to false), in particular the tables and columns in them involved
     */
    private val aggregatedFailedWhere: MutableMap<String, Set<String>> = mutableMapOf()


    fun copy(): FitnessValue {
        val copy = FitnessValue(size)
        copy.targets.putAll(this.targets)
        copy.extraToMinimize.putAll(this.extraToMinimize)
        copy.databaseExecutions.putAll(this.databaseExecutions) //note: DatabaseExecution supposed to be immutable
        copy.mongoExecutions.putAll(this.mongoExecutions)
        copy.aggregateDatabaseData()
        return copy
    }

    /**
     * We keep track of DB interactions per action.
     * However, there are cases in which we only care of aggregated data for all actions.
     * Instead of re-computing them each time, we just do it once and save the results
     */
    fun aggregateDatabaseData(){

        aggregatedFailedWhere.clear()
        aggregatedFailedWhere.putAll(DatabaseExecution.mergeData(
                databaseExecutions.values,
                {x ->  x.failedWhere}
        ))
    }

    fun setExtraToMinimize(actionIndex: Int, list: List<Double>) {
        extraToMinimize[actionIndex] = list.sorted()
    }

    fun setDatabaseExecution(actionIndex: Int, databaseExecution: DatabaseExecution){
        databaseExecutions[actionIndex] = databaseExecution
    }

    fun setMongoExecution(actionIndex: Int, mongoExecution: MongoExecution){
        mongoExecutions[actionIndex] = mongoExecution
    }

    fun isAnyDatabaseExecutionInfo() = databaseExecutions.isNotEmpty()

    fun getViewOfData(): Map<Int, Heuristics> {
        return targets
    }

    fun getViewOfAggregatedFailedWhere() = aggregatedFailedWhere

    fun doesCover(target: Int): Boolean {
        return targets[target]?.distance == MAX_VALUE
    }

    fun getHeuristic(target: Int): Double = targets[target]?.distance ?: 0.0


    fun computeFitnessScore(): Double {

        return targets.values.map { h -> h.distance }.sum()
    }

    fun computeFitnessScore(targetIds : List<Int>): Double {

        return targets.filterKeys { targetIds.contains(it)}.values.map { h -> h.distance }.sum()
    }

    fun coveredTargets(): Int {

        return targets.values.filter { t -> t.distance == MAX_VALUE }.count()
    }

    fun coveredTargets(prefix: String, idMapper: IdMapper) : Int{

        return targets.entries
                .filter { it.value.distance == MAX_VALUE }
                .filter { idMapper.getDescriptiveId(it.key).startsWith(prefix) }
                .count()
    }

    fun coverTarget(id: Int) {
        updateTarget(id, MAX_VALUE)
    }

    fun potentialFoundFaults(idMapper: IdMapper) : List<String>{
        return targets.keys
                .filter { idMapper.isFault(it)}
                .map { idMapper.getDescriptiveId(it) }
    }

    /**
     * Set the heuristic [value] for the given target [id].
     * If already existing, replace it only if better.
     */
    fun updateTarget(id: Int, value: Double, actionIndex : Int = -1) {

        if (value < 0 || value > MAX_VALUE) {
            throw IllegalArgumentException("Invalid value: $value")
        }

        val current = targets[id]

        if(current == null || value > current.distance) {
            targets[id] = Heuristics(value, actionIndex)
        }
    }

    /**
     * This only merges the target heuristics, and not the extra ones
     */
    fun merge(other: FitnessValue) {

        other.targets.keys.forEach { t ->
            val k = other.getHeuristic(t)
            if (k > this.getHeuristic(t)) {
                this.updateTarget(t, k)
            }
        }
    }

    /**
     * Check if current does subsume [other].
     * This means covering at least the same targets, and at least one better or
     * one more.
     *
     * Recall: during the search, we might not calculate all targets, eg once they
     * are covered.
     *
     * @param other, the one we compare to
     * @param targetSubset, only calculate subsumption on these testing targets
     */
    fun subsumes(
            other: FitnessValue,
            targetSubset: Set<Int>,
            strategy: EMConfig.SecondaryObjectiveStrategy,
            bloatControlForSecondaryObjective: Boolean)
            : Boolean {

        var atLeastOneBetter = false

        for (k in targetSubset) {

            val v = this.targets[k]?.distance ?: 0.0
            val z = other.targets[k]?.distance ?: 0.0
            if (v < z) {
                return false
            }

            val extra = compareExtraToMinimize(k, other, strategy)

            //FIXME this is inconsistent with what used in Archive. Should be
            //refactored, avoiding copy&paste
            if(bloatControlForSecondaryObjective){
                if (v > z ||
                        (v == z && this.size < other.size) ||
                        (v == z && this.size == other.size && extra > 0)) {
                    atLeastOneBetter = true
                }
            } else {
                if (v > z ||
                        (v == z && extra > 0) ||
                        (v == z && extra == 0 && this.size < other.size)) {
                    atLeastOneBetter = true
                }
            }
        }

        return atLeastOneBetter
    }

    /**
     * Check if current does differ from [other] regarding [targetSubset].
     *
     * Recall: during the search, we might not calculate all targets, eg once they
     * are covered.
     *
     * @param other, the one we compare to
     * @param targetSubset, only calculate subsumption on these testing targets
     */
    fun isDifferent(
            other: FitnessValue,
            targetSubset: Set<Int>,
            improved : MutableSet<Int>,
            different : MutableSet<Int>,
            strategy: EMConfig.SecondaryObjectiveStrategy,
            bloatControlForSecondaryObjective: Boolean) : Boolean {

        var atLeastOneDifferent = false
        for (k in targetSubset) {

            val v = this.targets[k]?.distance ?: 0.0
            val z = other.targets[k]?.distance ?: 0.0
            if (v == 0.0 && v == z)
                continue

            if (v != z) {
                different.add(k)
                atLeastOneDifferent = true
                if (v > z) improved.add(k)
                continue
            }

            val extra = compareExtraToMinimize(k, other, strategy)

            if (this.size != other.size || extra != 0) {
                different.add(k)
                atLeastOneDifferent = true

                if(bloatControlForSecondaryObjective){
                    if (this.size < other.size || (this.size == other.size && extra > 0)) {
                        improved.add(k)
                    }
                } else {
                    if (extra > 0 ||
                            (extra == 0 && this.size < other.size)) {
                        improved.add(k)
                    }
                }
            }
        }
        return atLeastOneDifferent
    }

    fun averageExtraDistancesToMinimize(actionIndex: Int): Double{
        return averageDistance(extraToMinimize[actionIndex])
    }

    /**
     * Compare the extra heuristics between this and [other].
     *
     * @return 0 if equivalent, 1 if this is better, and -1 otherwise
     */
    fun compareExtraToMinimize(
            target: Int,
            other: FitnessValue,
            strategy: EMConfig.SecondaryObjectiveStrategy)
            : Int {

        return when(strategy){
            AVG_DISTANCE -> compareAverage(target, other)
            AVG_DISTANCE_SAME_N_ACTIONS -> compareAverageSameNActions(target, other)
            BEST_MIN -> compareByBestMin(target, other)
        }
    }


    private fun isEmptyList(list: List<Double>?) : Boolean{
        return list == null || list.isEmpty()
    }

    private fun averageDistance(distances: List<Double>?): Double {
        if (isEmptyList(distances)) {
            //return 0.0
            throw IllegalArgumentException("Cannot compute average on empty list")
        }

        val sum = distances!!.map { v -> v / distances.size }.sum()

        return sum
    }

    private fun compareAverageSameNActions(target: Int, other: FitnessValue): Int {

        val thisAction = targets[target]?.actionIndex
        val otherAction = other.targets[target]?.actionIndex

        val thisN = databaseExecutions[thisAction]?.numberOfSqlCommands ?: 0
        val otherN = other.databaseExecutions[otherAction]?.numberOfSqlCommands ?: 0

        return when {
            thisN > otherN -> 1
            thisN < otherN -> -1
            else -> compareAverage(target, other)
        }
    }

    private fun compareAverage(target: Int, other: FitnessValue): Int {

        val thisAction = targets[target]?.actionIndex
        val otherAction = other.targets[target]?.actionIndex

        val thisDistances = this.extraToMinimize[thisAction]
        val otherDistances = other.extraToMinimize[otherAction]

        if(isEmptyList(thisDistances) && isEmptyList(otherDistances)){
            return 0
        }
        if(!isEmptyList(thisDistances) && isEmptyList(otherDistances)){
            return +1
        }
        if(isEmptyList(thisDistances) && !isEmptyList(otherDistances)){
            return -1
        }


        val ts = averageDistance(thisDistances)
        val os = averageDistance(otherDistances)

        return when {
            ts < os -> +1
            ts > os -> -1
            else -> 0
        }
    }


    private fun compareByBestMin(target: Int, other: FitnessValue): Int {

        val thisAction = targets[target]?.actionIndex
        val otherAction = other.targets[target]?.actionIndex

        val thisLength = this.extraToMinimize[thisAction]?.size ?: 0
        val otherLength = other.extraToMinimize[otherAction]?.size ?: 0
        val minLen = min(thisLength, otherLength)

        if (minLen > 0) {
            for (i in 0 until minLen) {
                val te = this.extraToMinimize[thisAction]!![i]
                val oe = other.extraToMinimize[otherAction]!![i]

                /*
                    We prioritize the improvement of lowest
                    heuristics, as more likely to be covered (ie 0)
                    first.
                */

                if (te < oe) {
                    return +1
                } else if (te > oe) {
                    return -1
                }
            }
        }

        val thisN = databaseExecutions[thisAction]?.numberOfSqlCommands ?: 0
        val otherN = other.databaseExecutions[otherAction]?.numberOfSqlCommands ?: 0

        /*
            if same min, reward number of SQL commands: if more, the
            better.
            If even with that we cannot make a choice, then reward
            the one with less heuristics, as that mean it had more
            success with the SQL commands
         */

        return when {
            thisN > otherN -> 1
            thisN < otherN -> -1
            thisLength > otherLength -> -1
            thisLength < otherLength -> 1
            else -> 0
        }
    }
}