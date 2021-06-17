package com.foo.graphql.interfaces.type

data class FlowerStore(
        override val id: Int? = null,
        override val name: String? = null
) : Store
