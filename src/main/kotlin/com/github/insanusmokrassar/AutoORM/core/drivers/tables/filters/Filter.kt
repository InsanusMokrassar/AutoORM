package com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters

val supportedFilters = listOf(
        "eq",
        "is",
        "gte",
        "gt",
        "lte",
        "lt",
        "in",
        "oneof"
)

class Filter(val field : String, val isOut : Boolean = false) {
    var filterName: String = ""
    set(value) {
        if (supportedFilters.contains(value)) {
            field = value
        } else {
            throw UnsupportedOperationException("This filter is not supported yet")
        }
    }
    var isNot = false
    var outClass : kotlin.reflect.KClass<*>?= null
    val args : MutableList<Any> = ArrayList()

    var logicalLink : String? = null

    init {
        if (field.isEmpty()) {
            throw IllegalArgumentException("Can't set empty field")
        }
    }

    fun isComplete() : Boolean {
        if (filterName.isEmpty() || (isOut && outClass == null)) {
            return false
        }
        return true
    }
}