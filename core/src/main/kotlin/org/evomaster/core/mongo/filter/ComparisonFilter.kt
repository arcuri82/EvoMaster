package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents where the value of the field name equals/is greater than/
 *  is greater/is less than/ is less than the specified value.
 */
class ComparisonFilter<V>(
        val fieldName: String,
        val operator: ComparisonQueryOperator,
        val value: V
) : ASTNodeFilter() {

    enum class ComparisonQueryOperator {
        EQUALS,
        GREATER_THAN,
        GREATER_THAN_EQUALS,
        LESS_THAN,
        LESS_THAN_EQUALS,
        NOT_EQUALS
    }

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}