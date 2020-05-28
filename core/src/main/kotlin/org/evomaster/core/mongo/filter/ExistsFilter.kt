package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents that contain the given field
 */
class ExistsFilter(
        val fieldName: String) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}