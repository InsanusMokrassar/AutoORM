package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.common

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.Transactable
import kotlin.reflect.KClass

interface TransactionContext : Transactable {
    fun <T : Any, M : Any> getTable(tableClass: KClass<T>, modelClass: KClass<M>): T
}