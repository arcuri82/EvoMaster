package com.foo.graphql.interfaces


import com.foo.graphql.interfaces.type.FlowerStore
import com.foo.graphql.interfaces.type.PotStore
import com.foo.graphql.interfaces.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {


    fun getStores(): List<Store> {
        return listOf(
                FlowerStore(0, "flowerStore0"),
                PotStore(1, "potStore1", "BlimBlim")
        )
    }


}