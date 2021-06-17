package com.foo.graphql.union.resolver


import com.foo.graphql.union.type.Store
import com.foo.graphql.union.DataRepository
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver{

    fun stores(): List<Store> {
        return dataRepo.getStores()
    }


}
