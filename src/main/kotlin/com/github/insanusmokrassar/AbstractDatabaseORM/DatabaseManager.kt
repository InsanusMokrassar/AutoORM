package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.TablesCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc.JDBCDatabaseDriver
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class DatabaseManager {

    protected val tablesCompiler = TablesCompiler()
    protected val databases : MutableList<DatabaseDriver> = ArrayList()

    init {
        databases.add(JDBCDatabaseDriver())
    }

    fun <T : Any, M : Any> getTable(tableClass : KClass<T>, modelClass : KClass<M>) : T {
        val realisation = tablesCompiler.getRealisation(tableClass)
        val result = realisation.constructors.getFirst {
            it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSubclassOf(TableProvider::class)
        }?.call(
                databases.getFirst {
                    it.supportTable(modelClass)
                }?.getProvider(modelClass)
        )
        if (result == null) {
            throw IllegalArgumentException("Can't find database which can contain or create this table")
        } else {
            return result
        }
    }
}

inline fun <T> Iterable<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}