package org.evomaster.core.mongo.filter

/**
 *  a filter that matches all documents containing a field that is an array where at least one member of the array matches the given filter.
 */
class ElemMatchFilter(
        val fieldName: String,
        val filter: ASTNodeFilter) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}