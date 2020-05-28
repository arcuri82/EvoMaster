package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents where the value of a field is an array of
 *  the specified size.
 */
class SizeFilter(
        val fieldName: String,
        val size: Int
) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}