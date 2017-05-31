package com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts

import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.PageFilter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.SearchQueryCompiler

val supportedFilters = listOf(
        "eq",
        "is",
        "gte",
        "gt",
        "lte",
        "lt",
        "in",
        "oneOf"
)

abstract class AbstractSearchQueryCompiler<T : Any> : SearchQueryCompiler<T> {
    var getFields : List<String>? = null
    val filters : MutableList<Filter> = ArrayList()
    var currentFilter: Filter? = null
    var pageFilter : PageFilter? = null

    override fun setNeededFields(vararg fieldNames: String): SearchQueryCompiler<T> {
        getFields = fieldNames.asList()
        return this
    }

    override fun setNeededFields(fieldNames: List<String>): SearchQueryCompiler<T> {
        getFields = fieldNames
        return this
    }

    override fun field(name: String, isOut : Boolean): SearchQueryCompiler<T> {
        if (currentFilter != null && !currentFilter!!.isComplete()) {
            throw IllegalStateException("Can't create next filter because last filter was not completed")
        }
        currentFilter = Filter(name, isOut)
        return this
    }

    override fun not(): SearchQueryCompiler<T> {
        if (currentFilter != null && currentFilter!!.isComplete()) {
            throw IllegalStateException("Can't edit current filter because it was completed")
        }
        currentFilter?.isNot = true
        return this
    }

    override fun filter(filterName: String, vararg params: Any) : SearchQueryCompiler<T> {
        if (!supportedFilters.contains(filterName)) {
            throw IllegalArgumentException("Unsupported filter \"$filterName\"")
        }
        currentFilter?.filterName = filterName
        currentFilter?.args?.addAll(params)
        return this
    }

    override fun linkWithNext(linkOperator: String): SearchQueryCompiler<T> {
        currentFilter?.logicalLink = linkOperator
        return this
    }

    override fun paging(page: Int, size: Int): SearchQueryCompiler<T> {
        pageFilter = PageFilter(page, size)
        return this
    }

    protected fun prepareToCompilingQuery() {
        val lastFilter = currentFilter
        lastFilter?.let {
            synchronized(it , {
                if (!filters.contains(it)) {
                    filters.add(it)
                }
            })
        }
        currentFilter = null
    }
}