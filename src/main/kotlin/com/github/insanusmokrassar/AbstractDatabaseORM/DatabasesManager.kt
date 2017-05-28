package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getFirst
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass

class DatabaseManager(config : IObject<Any>) {

    protected val databaseDrivers: MutableMap<String, DatabaseDriver> = HashMap()
    protected val databases: MutableMap<String, DatabaseConnect> = HashMap()
    protected val driversConfigs: List<IObject<Any>> = config.get<List<Any>>("drivers").filter {
        it is IObject<*>
    } as List<IObject<Any>>
    protected val databasesConfigs: List<IObject<Any>> = config.get<List<Any>>("databases").filter {
        it is IObject<*>
    } as List<IObject<Any>>

    //    fun getDatabase(name: String) : DatabaseConnect {
//        if (databases.containsKey(name)) {
//            return databases[name]!!
//        } else {
//
//        }
//    }

    fun getDatabaseConnect(name: String) : DatabaseConnect {
        if (databases.containsKey(name)) {
            return databases[name]!!
        } else {
            val config = databasesConfigs.getFirst {
                it.get<String>("name") == name
            }?: throw IllegalArgumentException("Can't find config for database $name")
            val driver = getDatabaseDriver(config.get("driver"))
            val connect = driver.getDatabaseConnect(config.get("config"))
            if (config.get("cache")) {
                databases.put(name, connect)
            }
            return connect
        }
    }

    protected fun getDatabaseDriver(name: String) : DatabaseDriver {
        if (databaseDrivers.containsKey(name)) {
            return databaseDrivers[name]!!
        } else {
            val config = driversConfigs.getFirst {
                it.get<String>("name") == name
            }?: throw IllegalArgumentException("Can't find config for database")
            val parameters = config.get<Any>("config")
            val driver = (Class.forName(config.get("classpath")).kotlin.constructors.getFirst {
                it.parameters.size == 1 && it.parameters[0].type.classifier as KClass<*> == parameters::class
            }?.call(
                    parameters
            )?: throw IllegalArgumentException("Can't find config for driver $name"))
                    as? DatabaseDriver ?: throw IllegalStateException("Founded driver for name $name is not DatabaseDriver")
            if (config.get("cache")) {
                databaseDrivers.put(name, driver)
            }
            return driver
        }
    }
//    protected fun <T : Any, M : Any> makeDatabaseFromConfig(tableClass: KClass<T>, modelClass: KClass<M>): TableProvider<M>? {
//        driversConfigs.forEach {
//            if (!databaseDrivers.contains(it.get("name"))) {
//                var currentDatabaseDriver: DatabaseDriver? = null
//                for (constructor in Class.forName(it.get("classpath")).kotlin.constructors.filter { it.parameters.size == 1 }) {
//                    try {
//                        val temp = (constructor as KFunction<DatabaseDriver>).call(it.get("driverParameters"))
//                        currentDatabaseDriver = temp
//                        break
//                    } catch (e: ClassCastException) {
//                        Logger.getGlobal().warning("Can't us \"$constructor\" for creating driver")
//                    }
//                }
//                if (currentDatabaseDriver == null) {
//                    Logger.getGlobal().warning("Unfortunately, I have not drivers which can be created for ${modelClass.simpleName}")
//                } else {
//                    if (it.get("cache")) {
//                        databaseDrivers.put(it.get("name"), currentDatabaseDriver)
//                    }
//                    if (currentDatabaseDriver.supportTable(modelClass)) {
//                        return@makeDatabaseFromConfig currentDatabaseDriver.getProvider(modelClass)
//                    }
//                }
//            }
//        }
//        return null
//    }
}