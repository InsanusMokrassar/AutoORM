package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import kotlin.reflect.KClass

interface DatabaseDriver {
    fun <T : Any> getProvider(modelClass : KClass<T>) : TableProvider<T>?
    fun supportTable(modelClass: KClass<*>) : Boolean
}