package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.TablesCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.Transactable
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getFirst
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class DatabaseConnect(private val driver: TableDriver, private val transactionManager: Transactable) : Transactable {
    var closed = false
    private set

    @Throws(IllegalArgumentException::class)
    fun <T : Any, M : Any, O : M> getTable(
            tableClass: KClass<T>,
            modelClass: KClass<M>,
            operationsClass: KClass<in O> = modelClass): T {
        val provider = driver.getTableProvider(modelClass, operationsClass)
        val realisation = TablesCompiler.getRealisation(tableClass)
        val result = realisation.constructors.getFirst {
            it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSubclassOf(TableProvider::class)
        }?.call(
                provider
        )?: throw IllegalArgumentException("Can't resolve table realisation")
        return result
    }

    override fun start() {
        transactionManager.start()
    }

    override fun abort() {
        transactionManager.abort()
    }

    override fun submit() {
        transactionManager.submit()
    }

    fun close() {
        driver.close()
        closed = true
    }
}