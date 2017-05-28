package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

import kotlin.reflect.KClass

interface TableDriver {
    fun <M : Any, O : M> getTableProvider(modelClass: KClass<M>, operationsClass: KClass<in O>) : TableProvider<M, O>
}