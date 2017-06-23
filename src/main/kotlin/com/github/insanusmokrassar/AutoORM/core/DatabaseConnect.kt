package com.github.insanusmokrassar.AutoORM.core

import com.github.insanusmokrassar.AutoORM.core.compilers.ClassCompiler
import com.github.insanusmokrassar.AutoORM.core.compilers.OperationsCompiler
import com.github.insanusmokrassar.AutoORM.core.compilers.TablesCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.ConnectionProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.Transactable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class DatabaseConnect(
        compiler: ClassCompiler,
        private val connectionProvider: ConnectionProvider,
        private val transactionDriver: Transactable,
        private val onFree: (DatabaseConnect) -> Unit = {},
        private val onClose: (DatabaseConnect) -> Unit = {}) : Transactable by transactionDriver {

    private val tablesCompiler: TablesCompiler = TablesCompiler(compiler)
    private val operationsCompiler: OperationsCompiler = OperationsCompiler(compiler)

    @Throws(IllegalArgumentException::class)
    fun <T : Any, M : Any, O : M> getTable(
            tableClass: KClass<T>,
            modelClass: KClass<M>,
            operationsClass: KClass<in O> = modelClass): T {
        val provider = getTableProvider(modelClass, operationsClass)
        val realisation = tablesCompiler.getRealisation(tableClass, modelClass)
        val result = realisation.constructors.first {
            it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSubclassOf(TableProvider::class)
        }?.call(
                provider
        )?: throw IllegalArgumentException("Can't resolve table realisation")
        return result
    }

    fun <M : Any, O : M> getTableProvider(
            modelClass: KClass<M>,
            operationsClass: KClass<in O> = modelClass): TableProvider<M, O> {
        return connectionProvider.getTableProvider(operationsCompiler, modelClass, operationsClass)
    }

    fun free() {
        onFree(this)
    }

    fun close() {
        connectionProvider.close()
        onClose(this)
    }
}