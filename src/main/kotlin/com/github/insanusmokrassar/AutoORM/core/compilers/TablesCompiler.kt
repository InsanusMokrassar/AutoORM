package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts.SearchQueryCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import kotlin.reflect.*

private class OverrideFunctionInfo(val function: KFunction<*>) : OverrideInfo{
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

object TablesCompiler {
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
        addImports(Filter::class, headerBuilder)
        addImports(ArrayList::class, headerBuilder)
        addImports(Collection::class, headerBuilder)
        addImports(SearchQueryCompiler::class, headerBuilder)

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

        val aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(
                interfaceImplementerClassNameTemplate(tableInterfaceClass.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!, tableInterfaceClass.isInterface())
        )

        return (aClass.kotlin as KClass<out T>)
    }
}
