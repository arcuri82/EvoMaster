package org.evomaster.core.mongo.filter

import org.bson.BsonType

/**
 *  A filter that matches all documents where the value of a field is an array of
 *  the specified size.
 */
class TypeFilter(
        val fieldName: String,
        val type: BsonType
) : ASTNodeFilter() {

    override fun <T, K> accept(visitor: FilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}