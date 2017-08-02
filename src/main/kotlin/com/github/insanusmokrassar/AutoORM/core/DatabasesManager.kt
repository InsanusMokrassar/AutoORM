package com.github.insanusmokrassar.AutoORM.core

import com.github.insanusmokrassar.AutoORM.*
import com.github.insanusmokrassar.AutoORM.core.generators.DefaultRealisationsGenerator
import com.github.insanusmokrassar.AutoORM.core.generators.RealisationsGenerator
import com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces.DatabaseProvider
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

@Throws(IllegalArgumentException::class)
fun createDatabasesPool(config : IObject<Any>): Map<String, ConnectionsPool> {
    val driversConfigs: List<IObject<Any>> = config.get<List<Any>>(driversField).filter {
        it is IObject<*>
    } as List<IObject<Any>>
    val databasesConfigs: List<IObject<Any>> = config.get<List<Any>>(databasesField).filter {
        it is IObject<*>
    } as List<IObject<Any>>

    val databaseDrivers: MutableMap<String, DatabaseProvider> = HashMap()
    val databasesPools = HashMap<String, ConnectionsPool>()

    val compiler = loadCompiler(config)

    databasesConfigs.forEach {
        val provider = getDatabaseProvider(it.get(driverField), databaseDrivers, driversConfigs)
        val currentConfig = it
        databasesPools.put(
                it.get<String>(nameField),
                ConnectionsPool {
                    onFree: (DatabaseConnect) -> Unit,
                    onClose: (DatabaseConnect) -> Unit ->
                    if (currentConfig.keys().contains(connectionsField)) {
                        val connections = ArrayList<DatabaseConnect>()
                        for (i: Int in 0..currentConfig.get<Int>(connectionsField) - 1) {
                            connections.add(
                                    provider.getDatabaseConnect(
                                            currentConfig.get(configField),
                                            compiler,
                                            onFree,
                                            onClose
                                    )
                            )
                        }
                        connections
                    } else {
                        listOf(provider.getDatabaseConnect(
                                currentConfig.get(configField),
                                compiler,
                                onFree,
                                onClose
                        ))
                    }
                }
        )
    }
    return databasesPools
}

fun loadCompiler(config: IObject<Any>): RealisationsGenerator {
    var compilerConfig: IObject<Any>?
    try {
        compilerConfig = config.get<IObject<Any>>(classesCompilerField)
    } catch (e: Exception) {
        compilerConfig = null
    }
    if (compilerConfig == null) {
        return DefaultRealisationsGenerator()
    } else {
        val config: Any?
        if (compilerConfig.keys().contains(configField)) {
            config = compilerConfig.get<Any>(configField)
        } else {
            config = null
        }
        val compilerClass = Class.forName(compilerConfig.get(classpathField)).kotlin as KClass<out RealisationsGenerator>
        if (config == null) {
            try {
                return compilerClass.constructors.first {
                    it.parameters.isEmpty()
                }.call()
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Can't find empty constructor for compiler without args", e)
            }
        } else {
            try {
                return compilerClass.constructors.first {
                    it.parameters.size == 1 && it.parameters[0].type.classifier == config::class
                }.call()
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Can't find empty constructor for compiler without args", e)
            }
        }
    }
}

private fun getDatabaseProvider(
        name: String,
        databaseDrivers:
        MutableMap<String, DatabaseProvider>,
        driversConfigs: List<IObject<Any>>) : DatabaseProvider {
    if (databaseDrivers.containsKey(name)) {
        return databaseDrivers[name]!!
    } else {
        try {
            val config = driversConfigs.first {
                it.get<String>(nameField) == name
            }
            val parameters = config.get<Any>(configField)
            val driver = (Class.forName(config.get(classpathField)).kotlin.constructors.first {
                it.parameters.size == 1 && (it.parameters[0].type.classifier as kotlin.reflect.KClass<*>).isSuperclassOf(parameters::class)
            }.call(
                    parameters
            )?: throw IllegalArgumentException("Can't find config for tableDriver $name"))
                    as? DatabaseProvider ?: throw IllegalStateException("Founded tableDriver for name $name is not AbstractDatabaseProvider")
            databaseDrivers.put(name, driver)
            return driver
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Can't find config for database")
        }
    }
}
