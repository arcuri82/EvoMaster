package com.foo.graphql.interfaces

import com.foo.graphql.SpringController



class InterfacesController : SpringController(GQLInterfacesApplication::class.java) {

    override fun schemaName() = GQLInterfacesApplication.SCHEMA_NAME


}
