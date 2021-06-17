package com.foo.graphql.unionInternal.type

data class Pot(
        override var id: Int? = null,
        val color: String? = null,
        val size: Int? = null
) : Bouquet
