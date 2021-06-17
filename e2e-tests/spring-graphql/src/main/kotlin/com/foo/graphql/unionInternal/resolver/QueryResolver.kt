package com.foo.graphql.unionInternal.resolver



import com.foo.graphql.unionInternal.DataRepository
import com.foo.graphql.unionInternal.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver (
    private val dataRepo: DataRepository
    ) : GraphQLQueryResolver{

    fun stores(): Store {
        return dataRepo.getStores()
    }


}
