package com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces

import kotlin.reflect.KClass

interface TableDriver {
    fun <M : Any, O : M> getTableProvider(modelClass: KClass<M>, operationsClass: KClass<in O> = modelClass) : TableProvider<M, O>
    fun close()
}