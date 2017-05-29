package com.github.insanusmokrassar.AbstractDatabaseORM

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getFirst
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class DatabaseManager(config : IObject<Any>) {

    private val databaseDrivers: MutableMap<String, DatabaseDriver> = HashMap()
    private val databaseConnections: MutableMap<String, DatabaseConnect> = HashMap()
    private val driversConfigs: List<IObject<Any>> = config.get<List<Any>>("drivers").filter {
        it is IObject<*>
    } as List<IObject<Any>>
    private val databasesConfigs: List<IObject<Any>> = config.get<List<Any>>("databases").filter {
        it is IObject<*>
    } as List<IObject<Any>>

    fun getDatabaseConnect(name: String) : DatabaseConnect {
        if (databaseConnections.containsKey(name)) {
            val connect = databaseConnections[name]!!
            if (connect.closed) {
                databaseConnections.remove(name)
                return getDatabaseConnect(name)
            }
            return connect
        } else {
            val config = databasesConfigs.getFirst {
                it.get<String>("name") == name
            }?: throw IllegalArgumentException("Can't find config for database $name")
            val driver = getDatabaseDriver(config.get("driver"))
            val connect = driver.getDatabaseConnect(config.get("config"))
            databaseConnections.put(name, connect)
            return connect
        }
    }

    private fun getDatabaseDriver(name: String) : DatabaseDriver {
        if (databaseDrivers.containsKey(name)) {
            return databaseDrivers[name]!!
        } else {
            val config = driversConfigs.getFirst {
                it.get<String>("name") == name
            }?: throw IllegalArgumentException("Can't find config for database")
            val parameters = config.get<Any>("config")
            val driver = (Class.forName(config.get("classpath")).kotlin.constructors.getFirst {
                it.parameters.size == 1 && (it.parameters[0].type.classifier as KClass<*>).isSuperclassOf(parameters::class)
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
}