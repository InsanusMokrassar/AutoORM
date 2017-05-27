package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.interfaces

interface TableProvider<T : Any> {
    fun insert(what : T) : Boolean
    fun update(than : T, where : SearchQueryCompiler<out Any>) : Boolean
    fun remove(where : SearchQueryCompiler<out Any>) : Boolean
    fun find(where : SearchQueryCompiler<out Any>) : Collection<T>
    fun getEmptyQuery() : SearchQueryCompiler<out Any>
}