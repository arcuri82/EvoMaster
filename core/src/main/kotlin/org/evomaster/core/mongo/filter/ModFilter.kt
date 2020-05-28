package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents where the value of a field divided by a divisor
 *  has the specified remainder (i.e. perform a modulo operation to select documents)
 */
class ModFilter(
        val fieldName: String,
        val divisor: Long,
        val remainder: Long
) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}