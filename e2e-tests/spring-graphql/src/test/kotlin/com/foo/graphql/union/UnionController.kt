package com.foo.graphql.union

import com.foo.graphql.SpringController



class UnionController : SpringController(GQLUnionApplication::class.java) {

    override fun schemaName() = GQLUnionApplication.SCHEMA_NAME


}
