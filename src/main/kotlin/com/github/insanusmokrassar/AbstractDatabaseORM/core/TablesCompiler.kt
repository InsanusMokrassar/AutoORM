package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleTableRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable
import kotlin.reflect.KClass

class TablesCompiler {
    protected val compiledMap : MutableMap<KClass<out Any>, Class<out Any>> = HashMap()

    init {
        compiledMap.put(ExampleTable::class, ExampleTableRealisation::class.java)
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