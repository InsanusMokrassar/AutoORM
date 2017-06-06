package com.github.insanusmokrassar.AutoORM.core.drivers.tables

import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.PageFilter

class SearchQuery {
    val fields: MutableList<String> = ArrayList()
    val filters : MutableList<Filter> = ArrayList()
    var pageFilter : PageFilter? = null
}
