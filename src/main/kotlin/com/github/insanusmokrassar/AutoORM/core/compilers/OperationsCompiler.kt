package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts.SearchQueryCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod

private fun getterTemplate(fieldProperty: KProperty<*>): String {
    val overrideBuilder = StringBuilder()
    if (!fieldProperty.isNullable()) {
        overrideBuilder.append("@${NotNull::class.simpleName}\n")
    } else {
        overrideBuilder.append("@${Nullable::class.simpleName}\n")
    }
    overrideBuilder
            .append("public ${fieldProperty.toJavaPropertyString()} get${fieldProperty.name[0].toUpperCase()}${fieldProperty.name.substring(1)}() {\n")
            .append("    return ${fieldProperty.name};\n}")
    return overrideBuilder.toString()
}

private fun setterTemplate(fieldProperty: KProperty<*>): String {
    val overrideBuilder = StringBuilder()
    if (!fieldProperty.isNullable()) {
        overrideBuilder.append("@${NotNull::class.simpleName}\n")
    }
    overrideBuilder
            .append("public void set${fieldProperty.name[0].toUpperCase()}${fieldProperty.name.substring(1)}(${fieldProperty.toJavaPropertyString()} ${fieldProperty.name}) {\n")
            .append("    this.${fieldProperty.name} = ${fieldProperty.name};\n}")
    return overrideBuilder.toString()
}

private fun overrideVariableTemplate(fieldProperty: KProperty<*>): String {
    val overrideBuilder = StringBuilder()
    overrideBuilder.append("${privateFieldTemplate(fieldProperty)}\n\n${getterTemplate(fieldProperty)}\n")

    if (fieldProperty.isMutable()) {
        overrideBuilder.append(setterTemplate(fieldProperty))
    }

    return overrideBuilder.toString()
}

private fun primaryKeyFindBuilder(primaryFields: List<KCallable<*>>): String {
    val findBuilder = StringBuilder()
    primaryFields.forEach {
        findBuilder.append(".field(\"${it.name}\", false).filter(\"eq\", ${it.name})")
        if (!primaryFields.isLast(it)) {
            findBuilder.append(".linkWithNext(\"and\")")
        }
    }
    return findBuilder.toString()
}

private val methodsBodies = mapOf(
        Pair(
                "refresh",
                {
                    method: KFunction<*>, whereFrom: KClass<*>, primaryFields: List<KCallable<*>> ->
                    val namesStack = Stack<String>()
                    val argsStack = Stack<String>()
                    primaryFields.forEach {
                        namesStack.push("is")
                        namesStack.push(it.name)
                        if (!primaryFields.isLast(it)) {
                            namesStack.push("and")
                        }
                        argsStack.push(it.name)
                    }
                    namesStack.push("where")
                    val methodBody = StringBuilder()
                    methodBody.append(
                            constructSearchQuery(
                                    whereFrom,
                                    OverridePseudoInfo(namesStack, argsStack)
                            )
                    )
                    methodBody.append("${whereFrom.simpleName} $resultName = ")
                            .append(
                                    resultResolver(
                                            Collection::class,
                                            whereFrom,
                                            "$providerVariableName.find($searchQueryName)"
                                    )
                            )
                            .append(";\n")
                    whereFrom.getVariables().forEach {
                        methodBody.append("this.${it.name} = $resultName.${it.getter.javaMethod!!.name}();\n")
                    }
                    methodOverrideTemplate(
                            method,
                            methodBody.toString(),
                            whereFrom.isInterface()
                    )
                }
        )
//        Pair(
//                "update",
//                {
//                    _: KClass<*>, primaryFields: List<KCallable<*>> ->
//                    val queryBuilder = StringBuilder()
//                    queryBuilder
//                            .append("public void update() {\n")
//                            .append("$providerVariableName.update(this, $providerVariableName.getEmptyQuery()${primaryKeyFindBuilder(primaryFields)});\n")
//                    queryBuilder.append("}")
//                }
//        ),
//        Pair(
//                "insert",
//                {
//                    _: KClass<*>, _: List<KCallable<*>> ->
//                    val queryBuilder = StringBuilder()
//                    queryBuilder
//                            .append("public void insert() {\n")
//                            .append("$providerVariableName.insert(this);\n")
//                    queryBuilder.append("}")
//                }
//        ),
//        Pair(
//                "remove",
//                {
//                    _: KClass<*>, primaryFields: List<KCallable<*>> ->
//                    val queryBuilder = StringBuilder()
//                    queryBuilder
//                            .append("public void remove() {\n")
//                            .append("$providerVariableName.remove($providerVariableName.getEmptyQuery()${primaryKeyFindBuilder(primaryFields)});\n")
//                    queryBuilder.append("}")
//                }
//        )
)

private class OverridePseudoInfo(val presetNames: Stack<String>, val presetArgsNames: Stack<String>, override val returnClass: KClass<*> = Unit::class): OverrideInfo {
    override val nameStack: Stack<String> = Stack()
    override val argsNamesStack: Stack<String> = Stack()
    init {
        refreshStacks()
    }

    override fun refreshStacks() {
        nameStack.addAll(presetNames)
        argsNamesStack.addAll(presetArgsNames)
    }

}

object OperationsCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    fun <O : Any> getRealisation(what : KClass<in O>) : KClass<out O> {
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
        val primaryFields = whereFrom.getPrimaryFields()

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")
        variablesToOverride.forEach {
            classBodyBuilder.append("${overrideVariableTemplate(it)}\n")
        }

        classBodyBuilder.append("${createConstructorForProperties(whereFrom, constructorVariables)}\n")

        methodsToOverride.forEach {
            classBodyBuilder.append("${methodsBodies[it.name]!!(it, whereFrom, primaryFields)}\n")
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

        val aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(
                interfaceImplementerClassNameTemplate(whereFrom.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName, whereFrom.isInterface()))

        return (aClass.kotlin as KClass<out O>)
    }
}
