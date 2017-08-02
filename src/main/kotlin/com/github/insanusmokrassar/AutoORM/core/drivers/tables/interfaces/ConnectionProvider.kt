package com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces

import com.github.insanusmokrassar.AutoORM.core.generators.RealisationsGenerator
import kotlin.reflect.KClass

interface ConnectionProvider {
    fun <M : Any, O : M> getTableProvider(generator: RealisationsGenerator, modelClass: KClass<M>, operationsClass: KClass<in O> = modelClass): TableProvider<M, O>
    fun close()
}