package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

interface TableProvider<M : Any> {
    fun insert(what : M) : Boolean
    fun update(than : M, where : SearchQueryCompiler<out Any>) : Boolean
    fun remove(where : SearchQueryCompiler<out Any>) : Boolean
    fun find(where : SearchQueryCompiler<out Any>) : Collection<M>
    fun getEmptyQuery() : SearchQueryCompiler<out Any>
}