package org.evomaster.core.mongo.filter

abstract class FilterVisitor<T, K>() {

    abstract fun visit(filter: ComparisonFilter<*>, argument: K): T;

    abstract fun visit(filter: AndFilter, argument: K): T

    abstract fun visit(filter: OrFilter, arg: K): T

    abstract fun visit(filter: AllFilter, arg: K): T

    abstract fun visit(filter: ElemMatchFilter, arg: K): T

    abstract fun visit(filter: SizeFilter, argument: K): T

    abstract fun visit(filter: InFilter, arg: K): T

    abstract fun visit(filter: NotInFilter, arg: K): T

    abstract fun visit(filter: NorFilter, arg: K): T

    abstract fun visit(filter: ExistsFilter, arg: K): T

    abstract fun visit(filter: NotExistsFilter, arg: K): T

    abstract fun visit(filter: RegexFilter, argument: K): T

    abstract fun visit(filter: SearchFilter, arg: K): T

    abstract fun visit(filter: WhereFilter, arg: K): T

    abstract fun visit(filter: ModFilter, argument: K): T

    abstract fun visit(filter: TypeFilter, argument: K): T

    abstract fun visit(filter: NotFilter, arg: K): T

}