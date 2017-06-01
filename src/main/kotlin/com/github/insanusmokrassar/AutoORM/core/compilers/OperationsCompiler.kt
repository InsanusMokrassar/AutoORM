package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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
                    whereFrom: KClass<*>, primaryFields: List<KCallable<*>> ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void refresh() {\n")
                            .append("${whereFrom.simpleName} result = ((List<${whereFrom.simpleName}>) $providerVariableName.find($providerVariableName.getEmptyQuery()${primaryKeyFindBuilder(primaryFields)})).get(0);\n")
                    whereFrom.getVariables().forEach {
                        if (it.isMutable()) {
                            queryBuilder.append("this.${it.name} = result.${it.name};\n")
                        }
                    }
                    queryBuilder.append("}")
                }
        ),
        Pair(
                "update",
                {
                    _: KClass<*>, primaryFields: List<KCallable<*>> ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void update() {\n")
                            .append("$providerVariableName.update(this, $providerVariableName.getEmptyQuery()${primaryKeyFindBuilder(primaryFields)});\n")
                    queryBuilder.append("}")
                }
        ),
        Pair(
                "insert",
                {
                    _: KClass<*>, _: List<KCallable<*>> ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void insert() {\n")
                            .append("$providerVariableName.insert(this);\n")
                    queryBuilder.append("}")
                }
        ),
        Pair(
                "remove",
                {
                    _: KClass<*>, primaryFields: List<KCallable<*>> ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void remove() {\n")
                            .append("$providerVariableName.remove($providerVariableName.getEmptyQuery()${primaryKeyFindBuilder(primaryFields)});\n")
                    queryBuilder.append("}")
                }
        )
)

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
            classBodyBuilder.append("${methodsBodies[it.name]!!(whereFrom, primaryFields)}\n")
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

        val aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(
                interfaceImplementerClassNameTemplate(whereFrom.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName, whereFrom.isInterface()))

        return (aClass.kotlin as KClass<out O>)
    }
}
