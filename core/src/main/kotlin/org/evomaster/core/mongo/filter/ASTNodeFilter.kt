package org.evomaster.core.mongo.filter

abstract class ASTNodeFilter {

    abstract fun <T,K> accept(visitor: FilterVisitor<T,K>, arg: K): T;
}