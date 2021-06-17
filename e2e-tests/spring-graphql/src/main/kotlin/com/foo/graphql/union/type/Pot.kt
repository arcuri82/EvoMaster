package com.foo.graphql.union.type

data class Pot(
        override var id: Int? = null,
        val color: String? = null,
        val size: Int? = null
) : Store
