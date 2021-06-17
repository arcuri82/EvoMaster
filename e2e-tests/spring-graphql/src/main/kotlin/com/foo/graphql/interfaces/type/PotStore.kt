package com.foo.graphql.interfaces.type

data class PotStore (
        override val id: Int? = null,
        override val name: String? = null,
        val address: String? = null
):Store
