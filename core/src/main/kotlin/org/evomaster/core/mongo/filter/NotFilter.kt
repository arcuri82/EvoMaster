package org.evomaster.core.mongo.filter

/**
 *  Creates a filter that matches all documents that do not match the passed in filter. Requires the field name to passed as part of the value passed in and lifts it to create a valid "$not" query:
 * not(eq("x", 1)) will generate a MongoDB query like:
 * {x : $not: {$eq : 1}}
 */
class NotFilter(
        val filter: ASTNodeFilter) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}