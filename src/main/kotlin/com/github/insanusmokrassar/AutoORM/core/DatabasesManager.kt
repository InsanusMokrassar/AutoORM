package com.github.insanusmokrassar.AutoORM.core

import com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.full.isSuperclassOf

class DatabaseManager(config : IObject<Any>) {

    private val databaseDrivers: MutableMap<String, DatabaseDriver> = HashMap()
    val databaseConnections: MutableMap<String, ConnectionsPool> = HashMap()
    private val driversConfigs: List<IObject<Any>> = config.get<List<Any>>("drivers").filter {
        it is IObject<*>
    } as List<IObject<Any>>
    private val databasesConfigs: List<IObject<Any>> = config.get<List<Any>>("databases").filter {
        it is IObject<*>
    } as List<IObject<Any>>

    init {
        databasesConfigs.forEach {
            val driver = getDatabaseDriver(it.get("driver"))
            val currentConfig = it
            databaseConnections.put(
                    it.get<String>("name"),
                    ConnectionsPool {
                        onFree: (DatabaseConnect) -> Unit,
                        onClose: (DatabaseConnect) -> Unit ->
                        if (currentConfig.keys().contains("connections")) {
                            val connections = ArrayList<DatabaseConnect>()
                            for (i: Int in 0..currentConfig.get<Int>("connections") - 1) {
                                connections.add(
                                        driver.getDatabaseConnect(
                                                currentConfig.get("config"),
                                                onFree,
                                                onClose
                                        )
                                )
                            }
                            connections
                        } else {
                            listOf(driver.getDatabaseConnect(
                                    currentConfig.get("config"),
                                    onFree,
                                    onClose
                            ))
                        }
                    }
            )
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
                it.parameters.size == 1 && (it.parameters[0].type.classifier as kotlin.reflect.KClass<*>).isSuperclassOf(parameters::class)
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