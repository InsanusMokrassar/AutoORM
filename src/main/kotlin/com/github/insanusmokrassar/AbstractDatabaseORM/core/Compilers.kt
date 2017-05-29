package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleOperationsRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleTableRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleOperations
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable
import kotlin.reflect.KClass

object TablesCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    init {
        compiledMap.put(ExampleTable::class, ExampleTableRealisation::class)
    }

    fun <T : Any> getRealisation(what : KClass<in T>) : KClass<T>  {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as KClass<T>
    }

    private fun <T : Any> compile(what : KClass<in T>) : KClass<T> {
        TODO()
    }
}

object ModelsCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, Class<out Any>> = HashMap()

    init {
        compiledMap.put(Example::class, ExampleRealisation::class.java)
    }

    fun <T : Any> getRealisation(what : KClass<in T>) : Class<out T>  {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as Class<T>
    }

    private fun <T : Any> compile(what : KClass<in T>) : Class<out T> {
        TODO()
    }
}

object OperationsCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, Class<out Any>> = HashMap()

    init {
        compiledMap.put(ExampleOperations::class, ExampleOperationsRealisation::class.java)
    }

    fun <T : Any> getRealisation(what : KClass<in T>) : Class<out T>  {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as Class<T>
    }

    private fun <T : Any> compile(what : KClass<in T>) : Class<out T> {
        TODO()
    }
}
