package org.evomaster.core.mongo

import org.bson.BsonArray
import org.bson.Document
import org.evomaster.core.mongo.filter.*
import org.evomaster.core.mongo.filter.ComparisonFilter.ComparisonQueryOperator.*
import java.lang.Integer.min
import java.util.stream.Collectors
import kotlin.math.abs

class MongoHeuristicCalculator : FilterVisitor<Double, Document>() {

    companion object {
        //2^16=65536, max distance for a char
        const val MAX_CHAR_DISTANCE = 65536
    }

    /**
     * Returns a double value that represents the distance
     * between two numbers
     */
    private fun computeDistance(a: Number, b: Number): Double {
        return abs(a.toDouble() - b.toDouble())
    }

    /**
     * Returns a double value that represents the distance
     * between two strings. The value is greater than 0.
     */
    private fun computeDistance(a: String, b: String): Double {
        val diff = abs(a.length - b.length).toLong()
        var dist: Long = diff * MAX_CHAR_DISTANCE
        for (i in 0 until min(a.length, b.length)) {
            dist += abs(a[i] - b[i]).toLong()
        }
        return dist.toDouble()
    }

    private fun computerComparison(dif: Double, operator: ComparisonFilter.ComparisonQueryOperator): Double {
        return when (operator) {
            EQUALS -> abs(dif)
            GREATER_THAN_EQUALS -> if (dif >= 0) 0.0 else -dif
            GREATER_THAN -> if (dif > 0) 0.0 else 1.0 - dif
            LESS_THAN_EQUALS -> if (dif <= 0) 0.0 else dif
            LESS_THAN -> if (dif < 0) 0.0 else 1.0 + dif
            NOT_EQUALS -> if (dif != 0.0) 0.0 else 1.0
        }
    }

    private fun computeDistance(x: Any?, y: Any?): Double {

        if (x != null && y != null) {
            if (x is Number && y is Number) {
                return computeDistance(x, y)
            } else if (x is String && y is String) {
                return computeDistance(x, y)
            } else {
                return Double.MAX_VALUE
            }
        } else if (x == null && y == null) {
            return 0.0
        } else {
            return Double.MAX_VALUE
        }
    }

    /**
     * Computes a heuristic value for a fieldName op value comparison.
     * If field was not defined in the document, or the data type
     * does not march the expected value, the comparison returns Double.MAX_VALUE.
     *
     * Otherwise computes the heuristic value as the SQL HeuristicsCalculator
     *
     */
    override fun visit(comparisonFilter: ComparisonFilter<*>, document: Document): Double {
        val fieldName = comparisonFilter.fieldName
        val expectedValue = comparisonFilter.value
        val operator = comparisonFilter.operator

        if (!document.containsKey(fieldName)) {
            // document has no field
            return Double.MAX_VALUE
        }

        val actualValue = document.getValue(fieldName)
        if (actualValue is Number && expectedValue is Number) {
            val x = actualValue.toDouble()
            val y = expectedValue.toDouble()
            val dif = x - y
            return computerComparison(dif, operator)
        }

        // unsupported data types
        return Double.MAX_VALUE
    }

    /**
     * The heuristic value for an AndFilter is the sum of all
     * distances for each inner filter.
     *
     * Notice that in case of overflow, Double.MAX_VALUE is returned.
     */
    override fun visit(andFilter: AndFilter, document: Document): Double {
        var distance = 0.0;
        andFilter.filters.forEach {
            distance += it.accept(this, document)
            if (distance < 0
                    || distance == Double.POSITIVE_INFINITY
                    || distance == Double.NEGATIVE_INFINITY) {
                // overflow
                return Double.MAX_VALUE
            }
        }
        return distance
    }

    /**
     * The heuristic value for an OrFilter is the minimum of all
     * distances for each filter in the Or formula.
     */
    override fun visit(orFilter: OrFilter, document: Document): Double {
        return orFilter.filters
                .stream()
                .map { it.accept(this, document) }
                .collect(Collectors.toList())
                .min() ?: Double.MAX_VALUE
    }


    /**
     * If the filter's field name is not found or the actual value
     * is not an array, returns the maximum Double.MAX_VALUE heuristic value.
     *
     * Otherwise, computes the distance of the actual array length
     * to the expected value.
     */
    override fun visit(sizeFilter: SizeFilter, document: Document): Double {
        val fieldName = sizeFilter.fieldName
        val expectedSize = sizeFilter.size

        if (!document.containsKey(fieldName)) {
            // document has no field
            return Double.MAX_VALUE
        }

        val value = document.get(fieldName)
        return if (value !is BsonArray) {
            Double.MAX_VALUE
        } else {
            computeDistance(expectedSize, value.size)
        }
    }

    /**
     * Computes the heuristic value for an InFilter. An InFilter selects
     * documents such that the value of the fieldName in the document
     * is an array such that it contains an expected value.
     *
     * The heuristic value is the minimum distance to the expected value
     * of all distances to elements in the array.
     *
     * If the fieldName is not defined or it is not an array, or if the
     * array is defined but empty, or no distance can be computed to
     * any element in the array (i.e. incompatible types), the heuristic
     * distance is Double.MAX_VALUE.
     */
    override fun visit(filter: InFilter, document: Document): Double {
        val fieldName = filter.fieldName
        val expectedValues = filter.values

        if (!document.containsKey(fieldName)) {
            // document has no field
            return Double.MAX_VALUE
        }

        val value = document.get(fieldName)

        return expectedValues
                .stream()
                .map { computeDistance(it, value) }
                .collect(Collectors.toList())
                .min() ?: Double.MAX_VALUE
    }

    /**
     * Returns the minimum distance of the expected fieldName
     * to field names in the document.
     * If no fields are defined in the document, it returns
     * the maximum double value.
     */
    override fun visit(filter: ExistsFilter, document: Document): Double {
        val expectedFieldName = filter.fieldName
        return document.keys
                .stream()
                .map { computeDistance(expectedFieldName, it) }
                .collect(Collectors.toList())
                .min() ?: Double.MAX_VALUE

    }

    override fun visit(allFilter: AllFilter, document: Document): Double {
        val fieldName = allFilter.fieldName
        val expectedValues = allFilter.values

        if (!document.containsKey(fieldName)) {
            // document has no field
            return Double.MAX_VALUE
        }

        val value = document.get(fieldName)
        if (value !is BsonArray) {
            return Double.MAX_VALUE
        }


        return expectedValues
                .stream()
                .map { computeDistance(it, value) }
                .collect(Collectors.toList())
                .min() ?: Double.MAX_VALUE

    }

    override fun visit(filter: NotInFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NorFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }


    override fun visit(filter: NotExistsFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: RegexFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: SearchFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: WhereFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: ModFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: TypeFilter, argument: Document): Double {
        return Double.MAX_VALUE
    }

    override fun visit(filter: NotFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }



    override fun visit(elemMatchFilter: ElemMatchFilter, arg: Document): Double {
        return Double.MAX_VALUE
    }

}