package com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts

import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.PageFilter

abstract class SearchQueryCompiler<out Out : Any> {
    val fields: MutableList<String> = ArrayList()
    val filters : MutableList<Filter> = ArrayList()
    var pageFilter : PageFilter? = null

    abstract fun compileQuery() : Out
    abstract fun compilePaging(): Out
}
