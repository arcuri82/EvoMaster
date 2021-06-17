package com.foo.graphql.union

import com.foo.graphql.union.type.Store
import com.foo.graphql.union.type.Flower
import com.foo.graphql.union.type.Pot
import org.springframework.stereotype.Component

@Component
open class DataRepository {

    fun getStores(): List<Store> {
        return listOf(
                Flower(0, "Calandiva", "pink",70),
                Pot(1, "pot1", 48)
        )
    }

}