package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.TablesCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.kotlinFunction

class DatabaseManager(config : IObject<Any>) {

    protected val tablesCompiler = TablesCompiler()
    protected val databaseDrivers: MutableMap<String, DatabaseDriver> = HashMap()
    protected val driversConfigs: List<IObject<Any>>

    init {
        driversConfigs = config.get<List<Any>>("drivers").filter {
            it is IObject<*>
        } as List<IObject<Any>>
    }

    fun <T : Any, M : Any> getTable(tableClass: KClass<T>, modelClass: KClass<M>): T {
        val realisation = tablesCompiler.getRealisation(tableClass)
        var provider = databaseDrivers.values.getFirst {
            it.supportTable(modelClass)
        }?.getProvider(modelClass)
        if (provider == null) {
            provider = makeDatabaseFromConfig(tableClass, modelClass)
        }
        val result = realisation.constructors.getFirst {
            it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSubclassOf(TableProvider::class)
        }?.call(
                provider
        )
        if (result == null) {
            throw IllegalArgumentException("Can't find database which can contain or create this table")
        } else {
            return result
        }
    }

    protected fun <T : Any, M : Any> makeDatabaseFromConfig(tableClass: KClass<T>, modelClass: KClass<M>): TableProvider<M>? {
        driversConfigs.forEach {
            if (!databaseDrivers.contains(it.get("name"))) {
                var currentDatabaseDriver: DatabaseDriver? = null
                for (constructor in Class.forName(it.get("classpath")).kotlin.constructors.filter { it.parameters.size == 1 }) {
                    try {
                        val temp = (constructor as KFunction<DatabaseDriver>).call(it.get("driverParameters"))
                        currentDatabaseDriver = temp
                        break
                    } catch (e: ClassCastException) {
                        Logger.getGlobal().warning("Can't us \"$constructor\" for creating driver")
                    }
                }
                if (currentDatabaseDriver == null) {
                    Logger.getGlobal().warning("Unfortunately, I have not drivers which can be created for ${modelClass.simpleName}")
                } else {
                    if (it.get("cache")) {
                        databaseDrivers.put(it.get("name"), currentDatabaseDriver)
                    }
                    if (currentDatabaseDriver.supportTable(modelClass)) {
                        return@makeDatabaseFromConfig currentDatabaseDriver.getProvider(modelClass)
                    }
                }
            }
        }
        return null
    }
}

inline fun <T> Iterable<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}

inline fun <T> Array<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}