package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.Example
import kotlin.reflect.KClass

class ObjectsCompiler {
    protected val compiledMap : MutableMap<KClass<out Any>, Class<out Any>> = HashMap()

    init {
        compiledMap.put(Example::class, ExampleRealisation::class.java)
    }

    fun <T : Any> getRealisation(what : KClass<T>) : Class<T>  {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as Class<T>
    }

    protected fun <T : Any> compile(what : KClass<T>) : Class<T> {
        throw UnsupportedOperationException("Now I can't compile classes :(")
    }
}