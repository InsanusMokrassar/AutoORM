package com.github.insanusmokrassar.AbstractDatabaseORM.core.compilers

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.isNullable
import com.github.insanusmokrassar.AbstractDatabaseORM.core.toJavaPropertyString
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import kotlin.reflect.*

val providerVariableName = "provider"

fun interfaceImplementerClassNameTemplate(whatFrom: String): String {
    return "${whatFrom}Impl"
}

fun importTemplate(what: String) : String {
    return "import $what;"
}

fun classImplementatorTemplate(header: String, classBody: String, whatFrom: String) : String {
    return "$header\n\npublic class ${interfaceImplementerClassNameTemplate(whatFrom)} implements $whatFrom {\n$classBody\n}"
}

val preparedImports: String = "${importTemplate(NotNull::class.qualifiedName!!)}\n${importTemplate(Nullable::class.qualifiedName!!)}\n${importTemplate(Override::class.qualifiedName!!)}\n${importTemplate(TableProvider::class.qualifiedName!!)}\n"

fun packageTemplate(what: String) : String {
    return "package $what;"
}

fun privateFieldTemplate(fieldProperty: KProperty<*>): String {
    return "private ${fieldProperty.toJavaPropertyString()} ${fieldProperty.name};"
}

fun privateFieldTemplate(fieldClass: KClass<*>, name: String): String {
    return "private ${fieldClass.toJavaPropertyString(false)} $name;"
}

fun createConstructorForProperties(whatFrom: KClass<*>, properties: List<KProperty<*>>) : String {
    val constructorBuilder = StringBuilder()
    constructorBuilder.append("public ${interfaceImplementerClassNameTemplate(whatFrom.simpleName!!)}(${TableProvider::class.toJavaPropertyString(false)} ${providerVariableName}")
    properties.forEach {
        val nullablePrefix: String
        if (it.isNullable()) {
            nullablePrefix = "@${Nullable::class.simpleName}"
        } else {
            nullablePrefix = ""
        }
        constructorBuilder.append(
                ", $nullablePrefix ${it.toJavaPropertyString()} ${it.name}"
        )
    }

    constructorBuilder
            .append(") {\n")
            .append("this.${providerVariableName} = ${providerVariableName};\n")
    properties.forEach {
        constructorBuilder.append("this.${it.name} = ${it.name};\n")
    }

    constructorBuilder.append("}")
    return constructorBuilder.toString()
}

fun addImports(from: KCallable<*>, to: StringBuilder) {
    val currentImport = importTemplate((from.returnType.classifier as KClass<*>).javaObjectType.canonicalName)
    if (!to.contains(currentImport)) {
        to.append("$currentImport\n")
    }
    from.parameters.filter {
        it.kind == KParameter.Kind.VALUE
    }.forEach {
        addImports(it, to)
    }
}

fun addImports(from: KParameter, to: StringBuilder) {
    val currentImport = importTemplate((from.type.classifier as KClass<*>).javaObjectType.canonicalName)
    if (!to.contains(currentImport)) {
        to.append("$currentImport\n")
    }
    from.type.arguments.forEach {
        addImports(it, to)
    }
}

fun addImports(from: KTypeProjection, to: StringBuilder) {
    val currentImport = importTemplate((from.type as KClass<*>).javaObjectType.canonicalName)
    if (!to.contains(currentImport)) {
        to.append("$currentImport\n")
    }
    from.type?.arguments?.forEach {
        addImports(it, to)
    }
}
