package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.filters

class Filter(val field : String, val isOut : Boolean = false, val previously : com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.filters.Filter? = null) {
    var filterName: String = ""
    var isNot = false
    var outClass : kotlin.reflect.KClass<*>?= null
    val args : MutableList<Any> = ArrayList()

    var logicalLink : String? = null
    var next : com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.filters.Filter? = null

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