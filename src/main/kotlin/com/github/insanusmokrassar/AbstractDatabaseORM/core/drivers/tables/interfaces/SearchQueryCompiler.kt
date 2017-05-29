package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

interface SearchQueryCompiler<Out : Any> {
    fun setNeededFields(vararg fieldNames : String) : SearchQueryCompiler<Out>
    fun setNeededFields(fieldNames: List<String>): SearchQueryCompiler<Out>

    fun field(name : String, isOut : Boolean = false) : SearchQueryCompiler<Out>

    fun not() : SearchQueryCompiler<Out>

    fun filter(filterName: String, vararg params : Any) : SearchQueryCompiler<Out>

    fun linkWithNext(linkOperator: String = "or"): SearchQueryCompiler<Out>

    fun paging(page: Int = 0, size: Int = 1): SearchQueryCompiler<Out>

    fun compileQuery() : Out
    fun compilePaging(): Out
}
