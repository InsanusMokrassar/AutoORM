package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleTableRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private fun importTemplate(what: String) : String {
    return "import $what;"
}

private val preparedImports: String = "${importTemplate(NotNull::class.qualifiedName!!)}\n${importTemplate(Nullable::class.qualifiedName!!)}\n${importTemplate(Override::class.qualifiedName!!)}\n${importTemplate(TableProvider::class.qualifiedName!!)}\n"

private fun packageTemplate(what: String) : String {
    return "package $what;"
}

private fun privateFieldTemplate(fieldProperty: KProperty<*>): String {
    return "private ${fieldProperty.toJavaPropertyString()} ${fieldProperty.name};"
}

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

private fun nullableGetterTemplate(fieldClass: KClass<*>, name: String): String {
    return "private ${fieldClass.simpleName} $name;"
}

//private fun classWithExtendsTemplate

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

object OperationsCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, Class<out Any>> = HashMap()

    init {
//        compiledMap.put(ExampleOperations::class, ExampleOperationsRealisation::class.java)
    }

    fun <T : Any> getRealisation(what : KClass<in T>) : Class<out T>  {
        if (!compiledMap.containsKey(what)) {
            compiledMap.put(what, compile(what))
        }
        return compiledMap[what] as Class<T>
    }

    private fun <T : Any> compile(what : KClass<in T>) : Class<out T> {
        if (!what.isAbstract) {
            throw IllegalArgumentException("Can't override not abstract class: nothing to override")
        }
        val headerBuilder = StringBuilder()
                .append(packageTemplate(what.getPackage()))
        val variablesToOverride = what.getVariablesToOverride()
        val methodsToOverride = what.getMethodsToOverride()
        val constructorVariables = what.getRequiredInConstructor()

        val classBodyBuilder = StringBuilder()
        variablesToOverride.forEach {
            classBodyBuilder.append("${overrideVariableTemplate(it)}\n")
        }
        TODO()
    }
}
