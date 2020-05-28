package org.evomaster.core.mongo.filter

/**
 * A filter that preforms a logical OR of the provided list of filters.
 */
class NorFilter(val filters: List<ASTNodeFilter>): ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, arg: K): T {
        return visitor.visit(this, arg)
    }

}