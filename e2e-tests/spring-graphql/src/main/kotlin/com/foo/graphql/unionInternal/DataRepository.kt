package com.foo.graphql.unionInternal


import com.foo.graphql.unionInternal.type.Store
import com.foo.graphql.unionInternal.type.Flower
import com.foo.graphql.unionInternal.type.Pot
import org.springframework.stereotype.Component

@Component
open class DataRepository {


    fun getStores(): Store {
        return Store(listOf(Flower(5, "Calandiva", "Red", 10), Pot(10, "Green", 90)))


    }

}