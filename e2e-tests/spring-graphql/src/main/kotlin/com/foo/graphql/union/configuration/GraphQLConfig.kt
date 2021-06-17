package com.foo.graphql.union.configuration

import com.foo.graphql.union.type.Flower
import com.foo.graphql.union.type.Pot
import graphql.kickstart.tools.SchemaParserDictionary
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLConfig() {
    @Bean
 open   fun schemaParserDictionary(): SchemaParserDictionary {
        return SchemaParserDictionary()
                .add(Flower::class)
                .add(Pot::class)

    }
}
