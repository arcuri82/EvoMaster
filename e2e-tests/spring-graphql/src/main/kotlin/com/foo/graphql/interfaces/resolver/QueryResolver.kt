package com.foo.graphql.interfaces.resolver


import com.foo.graphql.interfaces.type.Store
import com.foo.graphql.interfaces.DataRepository
import com.foo.graphql.interfaces.type.FlowerStore
import com.foo.graphql.interfaces.type.PotStore
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver {

    fun stores(): List<Store> {
        return dataRepo.getStores()
    }

}
