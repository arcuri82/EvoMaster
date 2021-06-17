package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.__TypeKind

/**
 * Intermediate data structure to parse and organize the object graphQl-schema types
 * */
class Table(
        /**
         * Describing the name of the table type
         */
        var tableType: String? = null,
        /**
         * Describing the name of the table field
         */
        var tableField: String = "",
        /**
         * Describing the kind of the tableField, eg: LIST
         */
        var kindOfTableField: __TypeKind? = null,
        /**
         * Describing if the kind of the table field is nullable
         */
        var isKindOfTableFieldOptional: Boolean = false,
        /**
         * Describing the type of the table field
         */
        var tableFieldType: String = "",
        /**
         * Describing the kind of the tableFieldType, eg: SCALAR, OBJECT,INPUT_OBJECT, ENUM
         */
        var kindOfTableFieldType: __TypeKind? = null,
        /**
         * Describing if the kind of the table field type is nullable
         */
        var isKindOfTableFieldTypeOptional: Boolean = false,
        /**
         * Describing if the table field has arguments
         */
        var tableFieldWithArgs: Boolean = false,

        /*
         * Containing the enum values
         */
        var enumValues: MutableList<String> = mutableListOf(),
        /*
         * Containing the union possible types
         */
        var unionTypes: MutableList<String> = mutableListOf(),
        /*
         * Containing the interface possible types
          */
        var interfaceTypes: MutableList<String> = mutableListOf()

)