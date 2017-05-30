package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.example.ExampleTableRealisation
import com.github.insanusmokrassar.AbstractDatabaseORM.example.UserInterfaces.ExampleTable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import kotlin.reflect.*
import jdk.nashorn.internal.codegen.CompilerConstants.className
import net.openhft.compiler.CompilerUtils



private val providerVariableName = "provider"

private fun interfaceImplementerClassNameTemplate(whatFrom: String): String {
    return "${whatFrom}Impl"
}

private fun importTemplate(what: String) : String {
    return "import $what;"
}

private fun classImplementatorTemplate(header: String, classBody: String, whatFrom: String) : String {
    return "$header\n\npublic class ${interfaceImplementerClassNameTemplate(whatFrom)} implements $whatFrom {\n$classBody\n}"
}

private val preparedImports: String = "${importTemplate(NotNull::class.qualifiedName!!)}\n${importTemplate(Nullable::class.qualifiedName!!)}\n${importTemplate(Override::class.qualifiedName!!)}\n${importTemplate(TableProvider::class.qualifiedName!!)}\n"

private fun packageTemplate(what: String) : String {
    return "package $what;"
}

private fun privateFieldTemplate(fieldProperty: KProperty<*>): String {
    return "private ${fieldProperty.toJavaPropertyString()} ${fieldProperty.name};"
}

private fun privateFieldTemplate(fieldClass: KClass<*>, name: String): String {
    return "private ${fieldClass.toJavaPropertyString(false)} $name;"
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

private fun createConstructorForProperties(whatFrom: KClass<*>, properties: List<KProperty<*>>) : String {
    val constructorBuilder = StringBuilder()
    constructorBuilder.append("public ${interfaceImplementerClassNameTemplate(whatFrom.simpleName!!)}(${TableProvider::class.toJavaPropertyString(false)} $providerVariableName")
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
            .append("this.$providerVariableName = $providerVariableName;\n")
    properties.forEach {
        constructorBuilder.append("this.${it.name} = ${it.name};\n")
    }

    constructorBuilder.append("}")
    return constructorBuilder.toString()
}

private fun addImports(from: KCallable<*>, to: StringBuilder) {
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

private fun addImports(from: KParameter, to: StringBuilder) {
    val currentImport = importTemplate((from.type.classifier as KClass<*>).javaObjectType.canonicalName)
    if (!to.contains(currentImport)) {
        to.append("$currentImport\n")
    }
    from.type.arguments.forEach {
        addImports(it, to)
    }
}

private fun addImports(from: KTypeProjection, to: StringBuilder) {
    val currentImport = importTemplate((from.type as KClass<*>).javaObjectType.canonicalName)
    if (!to.contains(currentImport)) {
        to.append("$currentImport\n")
    }
    from.type?.arguments?.forEach {
        addImports(it, to)
    }
}

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

private val methodsBodies = mapOf(
        Pair(
                "refresh",
                {
                    whereFrom: KClass<*>, primaryFieldName: String ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void refresh() {\n")
                            .append("${whereFrom.simpleName} result = ((List<${whereFrom.simpleName}>) $providerVariableName.find($providerVariableName.getEmptyQuery().field(\"$primaryFieldName\", false).filter(\"eq\", $primaryFieldName))).get(0);\n")
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
                    whereFrom: KClass<*>, primaryFieldName: String ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void update() {\n")
                            .append("$providerVariableName.update(this, $providerVariableName.getEmptyQuery().field(\"$primaryFieldName\", false).filter(\"eq\", $primaryFieldName));\n")
                    queryBuilder.append("}")
                }
        ),
        Pair(
                "insert",
                {
                    whereFrom: KClass<*>, primaryFieldName: String ->
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
                    whereFrom: KClass<*>, primaryFieldName: String ->
                    val queryBuilder = StringBuilder()
                    queryBuilder
                            .append("public void remove() {\n")
                            .append("$providerVariableName.remove($providerVariableName.getEmptyQuery().field(\"$primaryFieldName\", false).filter(\"eq\", $primaryFieldName));\n")
                    queryBuilder.append("}")
                }
        )
)

object OperationsCompiler {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    fun <O : Any> getRealisation(what : KClass<in O>) : KClass<out O>  {
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
        val primaryFieldName = whereFrom.getPrimaryFieldName()?.name

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")
        variablesToOverride.forEach {
            classBodyBuilder.append("${overrideVariableTemplate(it)}\n")
        }

        classBodyBuilder.append("${createConstructorForProperties(whereFrom, constructorVariables)}\n")

        methodsToOverride.forEach {
            classBodyBuilder.append("${methodsBodies[it.name]!!(whereFrom, primaryFieldName!!)}\n")
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
                classImplementatorTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName))

        return (aClass.kotlin as KClass<out O>)
    }
}
