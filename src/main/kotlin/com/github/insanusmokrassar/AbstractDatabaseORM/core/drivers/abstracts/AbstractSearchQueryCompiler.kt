package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.abstracts

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.filters.Filter
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.filters.PageFilter
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.interfaces.SearchQueryCompiler

abstract class AbstractSearchQueryCompiler<T : Any> : SearchQueryCompiler<T> {
    var getFields : List<String>? = null
    var currentFilter: Filter? = null
    var pageFilter = PageFilter()

    override fun setNeededFields(vararg fieldNames: String): SearchQueryCompiler<T> {
        getFields = fieldNames.asList()
        return this
    }

    override fun field(name: String, isOut : Boolean): SearchQueryCompiler<T> {
        currentFilter = Filter(name, isOut, currentFilter)
        currentFilter?.previously?.next = currentFilter
        return this
    }

    override fun not(): SearchQueryCompiler<T> {
        currentFilter?.isNot = true
        return this
    }

    override fun filter(filterName: String, vararg params: Any) : SearchQueryCompiler<T> {
        currentFilter?.filterName = filterName
        currentFilter?.args?.add(params.asList())
        return this
    }

    override fun linkWithNext(linkOperator: String): SearchQueryCompiler<T> {
        currentFilter?.logicalLink = linkOperator
        return this
    }

    override fun paging(page: Int, offset: Int, size: Int): SearchQueryCompiler<T> {
        pageFilter = PageFilter(page, offset, size)
        return this
    }
}