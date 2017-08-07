package com.github.insanusmokrassar.AutoORM.core

import com.github.insanusmokrassar.AutoORM.core.generators.RealisationsCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.ConnectionProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.Transactable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class DatabaseConnect(
        private val compiler: RealisationsCompiler,
        private val connectionProvider: ConnectionProvider,
        private val transactionDriver: Transactable,
        private val onFree: (DatabaseConnect) -> Unit = {},
        private val onClose: (DatabaseConnect) -> Unit = {}) : Transactable by transactionDriver {

    @Throws(IllegalArgumentException::class)
    fun <T : Any, M : Any, O : M> getTable(
            tableOperationsClass: KClass<T>,
            tableModelClass: KClass<M>,
            tableModelOperationsClass: KClass<in O> = tableModelClass): T {
        val provider = getTableProvider(tableModelClass, tableModelOperationsClass)
        val realisation = compiler.compile(tableOperationsClass, tableModelClass)
        val result = realisation.constructors.first {
            it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSubclassOf(TableProvider::class)
        }.call(
                provider
        )
        return result
    }

    fun <M : Any, O : M> getTableProvider(
            modelClass: KClass<M>,
            operationsClass: KClass<in O> = modelClass): TableProvider<M, O> {
        return connectionProvider.getTableProvider(compiler, modelClass, operationsClass)
    }

    fun free() {
        onFree(this)
    }

    fun close() {
        connectionProvider.close()
        onClose(this)
    }
}