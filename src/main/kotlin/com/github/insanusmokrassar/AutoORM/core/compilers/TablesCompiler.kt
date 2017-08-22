package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

private class OverrideFunctionInfo(override val function: KFunction<*>) : OverrideInfo{
    val nameParts = function.name.camelCaseWords()
    val args = function.parameters

    override val nameStack = Stack<String>()
    override val argsNamesStack = Stack<String>()
    override val returnClass: KClass<*> = function.returnClass()

    init {
        refreshStacks()
    }

    override fun refreshStacks() {
        nameStack.clear()
        argsNamesStack.clear()

        nameStack.addAll(nameParts.reversed())
        argsNamesStack.addAll(args.filter {
            it.kind != KParameter.Kind.INSTANCE
        }.select {
            it.name
        }.reversed())
    }
}

class TablesCompiler(private val compiler: ClassCompiler) {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    fun <T : Any> getRealisation(tableInterface: KClass<in T>, modelInterface: KClass<*>) : KClass<T> {
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

        val aClass = compiler.compile(
                interfaceImplementerClassNameTemplate(tableInterfaceClass.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!, tableInterfaceClass.isInterface()),
                tableInterfaceClass
        )

        return aClass
    }
}
