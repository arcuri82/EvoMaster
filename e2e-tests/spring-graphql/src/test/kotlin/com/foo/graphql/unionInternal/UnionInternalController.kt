package com.foo.graphql.unionInternal

import com.foo.graphql.SpringController


class UnionInternalController : SpringController(GQLUnionInternalApplication::class.java) {

    override fun schemaName() = GQLUnionInternalApplication.SCHEMA_NAME


}
