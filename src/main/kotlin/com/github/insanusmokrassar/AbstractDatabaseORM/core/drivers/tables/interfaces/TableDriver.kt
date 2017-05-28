package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

import kotlin.reflect.KClass

interface TableDriver {
    fun <M : Any> getTableProvider(modelClass: KClass<M>) : TableProvider<M>
}