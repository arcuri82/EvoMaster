package org.evomaster.core.mongo.filter

/**
 *  A filter that matches all documents where the value of the field matches the
 *  given regular expression pattern with the given options applied.
 */
class RegexFilter(
        val fieldName: String,
        val pattern: String,
        val options: String
) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}