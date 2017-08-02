package com.github.insanusmokrassar.AutoORM.core.generators

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import java.util.HashMap
import kotlin.reflect.KClass

class DefaultRealisationsGenerator : RealisationsGenerator {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    override fun <T : Any> getTableRealisation(tableInterface: KClass<in T>, modelInterface: KClass<*>) : KClass<T> {
        if (!compiledMap.containsKey(tableInterface)) {
            compiledMap.put(tableInterface, compile(tableInterface, modelInterface))
        }
        return compiledMap[tableInterface] as KClass<T>
    }

    private fun <T : Any> compile(tableInterfaceClass: KClass<in T>, modelInterfaceClass: KClass<*>) : KClass<out T> {
        if (!tableInterfaceClass.isAbstract || !tableInterfaceClass.constructors.isEmpty()) {
            throw IllegalArgumentException("Can't override not interface")
        }
        val methodsToOverride = tableInterfaceClass.getMethodsToOverride()

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")

        classBodyBuilder.append("${createConstructorForProperties(tableInterfaceClass, emptyList())}\n")

        val headerBuilder = StringBuilder()
                .append("${packageTemplate(tableInterfaceClass.getPackage())}\n")
                .append(preparedImports)
        methodsToOverride.forEach {
            addImports(it, headerBuilder)
        }
        addStandardImports(headerBuilder)

        methodsToOverride.forEach {
            val funcInfo = OverrideFunctionInfo(it)
            classBodyBuilder.append(
                    methodOverrideTemplate(
                            it,
                            functionCodeBuilder(modelInterfaceClass, funcInfo, operations[funcInfo.nameStack.pop()]!!),
                            tableInterfaceClass.isInterface()
                    )
            )
        }

        val aClass = compile(
                interfaceImplementerClassNameTemplate(tableInterfaceClass.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!, tableInterfaceClass.isInterface())
        )

        return aClass as KClass<out T>
    }

    override fun <O : Any> getModelOperationsRealisation(what: KClass<in O>): KClass<out O> {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as KClass<out O>
    }

    private fun <O : Any> compile(whereFrom: KClass<in O>) : KClass<out O> {
        if (!whereFrom.isAbstract) {
            throw IllegalArgumentException("Can't override not abstract class: nothing to override")
        }
        val variablesToOverride = whereFrom.getVariablesToOverride()
        val methodsToOverride = whereFrom.getMethodsToOverride()
        val constructorVariables = whereFrom.getRequiredInConstructor()

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")
        variablesToOverride.forEach {
            classBodyBuilder.append("${overrideVariableTemplate(it)}\n")
        }

        classBodyBuilder.append("${createConstructorForProperties(whereFrom, constructorVariables)}\n")

        methodsToOverride.forEach {
            classBodyBuilder.append("${operationsMethodsBodies[it.name]!!(it, whereFrom)}\n")
        }

        val headerBuilder = StringBuilder()
                .append("${packageTemplate(whereFrom.getPackage())}\n")
                .append(preparedImports)

        variablesToOverride.forEach {
            addImports(it, headerBuilder)
        }
        methodsToOverride.forEach {
            addImports(it, headerBuilder)
        }
        addStandardImports(headerBuilder)

        val aClass = compile(
                interfaceImplementerClassNameTemplate(whereFrom.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName, whereFrom.isInterface()))

        return aClass as KClass<out O>
    }

    fun compile(className: String, classCode: String) {
        CompilerUtils.CACHED_COMPILER.loadFromJava(
                className,
                classCode
        ).kotlin
    }
}