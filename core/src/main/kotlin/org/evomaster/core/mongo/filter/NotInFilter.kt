package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents where the value of a field does not equal any of the specified values or does not exist.
 */
class NotInFilter(
        val fieldName: String,
        val values: List<*>) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}