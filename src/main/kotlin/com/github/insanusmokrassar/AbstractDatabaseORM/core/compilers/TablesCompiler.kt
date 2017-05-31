package com.github.insanusmokrassar.AbstractDatabaseORM.core.compilers

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getMethodsToOverride
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getPackage
import kotlin.reflect.KClass

private val partsHandlers = mapOf(
        Pair(
                "find",
                {
                    "$providerVariableName."
                }
        )
)

object TablesCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    init {
//        compiledMap.put(ExampleTable::class, ExampleTableRealisation::class)
    }

    fun <T : Any> getRealisation(what : KClass<in T>) : KClass<T> {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as KClass<T>
    }

    private fun <T : Any> compile(whereFrom: KClass<in T>) : KClass<T> {
        if (!whereFrom.isAbstract || !whereFrom.constructors.isEmpty()) {
            throw IllegalArgumentException("Can't override not interface")
        }
        val methodsToOverride = whereFrom.getMethodsToOverride()

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")

        classBodyBuilder.append("${createConstructorForProperties(whereFrom, emptyList())}\n")



        val headerBuilder = StringBuilder()
                .append("${packageTemplate(whereFrom.getPackage())}\n")
                .append(preparedImports)
        methodsToOverride.forEach {
            addImports(it, headerBuilder)
        }
        TODO()
    }
}
