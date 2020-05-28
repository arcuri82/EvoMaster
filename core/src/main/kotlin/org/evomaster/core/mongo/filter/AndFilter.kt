package org.evomaster.core.mongo.filter

/**
 *  A filter that performs a logical AND of the provided list of filters.
 */
class AndFilter(val filters: List<ASTNodeFilter>): ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}