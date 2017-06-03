package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts.SearchQueryCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.logging.Logger
import kotlin.reflect.*
import kotlin.reflect.full.functions

val providerVariableName = "provider"

val searchQueryName = "searchQuery"
val filterName = "filter"
val resultName = "result"

val operations = mapOf(
        Pair(
                "find",
                "find"
        ),
        Pair(
                "select",
                "find"
        ),
        Pair(
                "search",
                "find"
        ),
        Pair(
                "get",
                "find"
        ),
        Pair(
                "insert",
                "insert"
        ),
        Pair(
                "add",
                "insert"
        ),
        Pair(
                "put",
                "insert"
        ),
        Pair(
                "update",
                "update"
        ),
        Pair(
                "remove",
                "remove"
        ),
        Pair(
                "delete",
                "remove"
        )
)

val pagingIdentifiers = mapOf(
        Pair(
                "all",
                {
                    _: OverrideInfo ->
                    ""
                }
        ),
        Pair(
                "first",
                {
                    _: OverrideInfo ->
                    "$searchQueryName.getPageFilter();\n"
                }
        )
)

val filtersArgsCounts = mapOf(
        Pair(
                "eq",
                1
        ),
        Pair(
                "is",
                1
        ),
        Pair(
                "gt",
                1
        ),
        Pair(
                "gte",
                1
        ),
        Pair(
                "lt",
                1
        ),
        Pair(
                "lte",
                1
        ),
        Pair(
                "in",
                2
        ),
        Pair(
                "oneOf",
                1
        )
)

val linksWithNextCondition = listOf(
        "or",
        "and"
)

val whereIdentifiersAlgorithms = mapOf(
        Pair(
                "where",
                {
                    funcInfo: OverrideInfo ->
                    constructWhere(funcInfo)
                }
        ),
        Pair(
                "By",
                {
                    funcInfo: OverrideInfo ->
                    constructWhere(funcInfo)
                }
        )
).plus(pagingIdentifiers)

fun interfaceImplementerClassNameTemplate(whatFrom: String): String {
    return "${whatFrom}Impl"
}

fun importTemplate(what: String) : String {
    return "import $what;"
}

fun classImplementerTemplate(header: String, classBody: String, whatFrom: String, whatFromIsInterface: Boolean = true) : String {
    if (whatFromIsInterface) {
        return "$header\n\npublic class ${interfaceImplementerClassNameTemplate(whatFrom)} implements $whatFrom {\n$classBody\n}"
    } else {
        return "$header\n\npublic class ${interfaceImplementerClassNameTemplate(whatFrom)} extends $whatFrom {\n$classBody\n}"
    }
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

fun addImports(from: KClass<*>, to: StringBuilder) {
    to.append("${importTemplate(from.javaObjectType.canonicalName)}\n")
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


fun constructWhere(funcInfo: OverrideInfo): String {
    val builder = StringBuilder()
    val filters = ArrayList<String>()
    while (!funcInfo.nameStack.empty() && !pagingIdentifiers.containsKey(funcInfo.nameStack.peek())) {
        val currentFilterBuilder = StringBuilder()
        if (filters.isEmpty()) {
            currentFilterBuilder.append("Filter $filterName;")
        }
        val fieldName = funcInfo.nameStack.pop()
        var filterOrOutField = funcInfo.nameStack.pop()
        val isOut = !filtersArgsCounts.keys.contains(filterOrOutField) && filterOrOutField != "not"
        currentFilterBuilder.append(
                "$filterName = new Filter(\"$fieldName\", $isOut);\n"
        )
        if (isOut) {
            filterOrOutField = funcInfo.nameStack.pop()
        } else if (filterOrOutField == "not") {
            filterOrOutField = funcInfo.nameStack.pop()
            currentFilterBuilder.append("$filterName.setNot(true);\n")
        }
        currentFilterBuilder.append("$filterName.setFilterName(\"$filterOrOutField\");\n")
        for (i: Int in 0..filtersArgsCounts[filterOrOutField]!! - 1) {
            currentFilterBuilder.append("$filterName.getArgs().add(${funcInfo.argsNamesStack.pop()});\n")
        }
        if (funcInfo.nameStack.isNotEmpty() && linksWithNextCondition.contains(funcInfo.nameStack.peek())) {
            currentFilterBuilder.append("$filterName.setLogicalLink(\"${funcInfo.nameStack.pop()}\");\n")
        }
        currentFilterBuilder.append("$searchQueryName.getFilters().add($filterName);\n")
        filters.add(currentFilterBuilder.toString())
    }

    for (currentFilterString in filters) {
        builder.append(currentFilterString)
    }

    if (!funcInfo.nameStack.empty() && pagingIdentifiers.containsKey(funcInfo.nameStack.peek())) {
        builder.append(pagingIdentifiers[funcInfo.nameStack.pop()]!!(funcInfo))
    }

    return builder.toString()
}

fun constructSearchQuery(modelInterfaceClass: KClass<*>, funcInfo: OverrideInfo): String {
    val neededFields = HashSet<String>()
    modelInterfaceClass.getRequiredInConstructor().forEach {
        neededFields.add(it.name)
    }
    while (funcInfo.nameStack.isNotEmpty() && !whereIdentifiersAlgorithms.keys.contains(funcInfo.nameStack.peek())) {
        neededFields.add(funcInfo.nameStack.pop())
    }
    val searchQueryBody = StringBuilder()
    searchQueryBody.append("${SearchQueryCompiler::class.simpleName} $searchQueryName = $providerVariableName.getEmptyQuery();\n")
    neededFields.forEach {
        searchQueryBody.append("$searchQueryName.getFields().add(\"$it\");\n")
    }
    try {
        searchQueryBody.append(whereIdentifiersAlgorithms[funcInfo.nameStack.pop()]!!(funcInfo))
    } catch (e: NullPointerException) {
        Logger.getGlobal().warning("Can't find where identifier. Use paging \'all\'")
        searchQueryBody.append(whereIdentifiersAlgorithms["all"]!!(funcInfo))
    }

    return searchQueryBody.toString()
}

fun resultResolver(from: KClass<*>, to: KClass<*>, resultVariable: String = resultName): String {
    when(from) {
        Collection::class -> when(to) {
            List::class -> return "new ${ArrayList::class.java.simpleName}($resultVariable)"
            Boolean::class -> return "!$resultVariable.isEmpty()"
            Unit::class -> return ""
            else -> "(${to.javaObjectType.simpleName})$resultVariable.toArray()[0]"
        }
        to -> return resultVariable
        else -> when(to) {
            Unit::class -> return ""
            else -> return "(${to.javaObjectType.simpleName}) $resultVariable"
        }
    }
    return ""
}

fun functionCodeBuilder(modelInterfaceClass: KClass<*>, funcInfo: OverrideInfo, operationName: String): String {
    val currentFunction = TableProvider::class.functions.getFirst{ it.name== operationName }!!
    val functionParams = currentFunction.parameters.filter { it.kind != KParameter.Kind.INSTANCE }
    val resultBuilder = StringBuilder()
    resultBuilder.append("${TableProvider::class.functions.getFirst { it.name== operationName }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.$operationName(")
    functionParams.forEach {
        val classifier = it.type.classifier
        when(classifier) {
            SearchQueryCompiler::class -> resultBuilder.append(searchQueryName)
            else -> {
                if (classifier is KTypeParameter && classifier.variance == KVariance.INVARIANT) {
                    resultBuilder.append(funcInfo.argsNamesStack.pop())
                }
            }
        }
        if (!functionParams.isLast(it)) {
            resultBuilder.append(", ")
        }
    }
    resultBuilder.append(");\n")

    val resolvedResult = resultResolver(
            TableProvider::class.functions.getFirst { it.name== operationName }!!.returnClass(),
            funcInfo.returnClass
    )
    if (resolvedResult.isNotEmpty()) {
        resultBuilder.append(
            "return $resolvedResult;\n"
        )
    }
    val builder = StringBuilder()
    if (functionParams.select({ it.type.classifier }).contains(SearchQueryCompiler::class)) {
        builder.append("${constructSearchQuery(modelInterfaceClass, funcInfo)}\n")
    }
    builder.append(resultBuilder)
    return builder.toString()
}

fun methodOverrideTemplate(method: KFunction<*>, methodBody: String, inInterface: Boolean = true): String {
    val methodBuilder = StringBuilder()
    if (!inInterface) {
        methodBuilder.append("@${Override::class.simpleName}\n")
    }
    val params = method.parameters
    methodBuilder.append("public ${method.returnClass().toJavaPropertyString(method.isNullable())} ${method.name}(")
    params.forEach {
        if (it.kind == KParameter.Kind.INSTANCE) {
            return@forEach
        }
        methodBuilder.append("${it.type.toJavaPropertyString()} ${it.name}")
        if (!params.isLast(it)) {
            methodBuilder.append(", ")
        }
    }
    methodBuilder.append(") {\n$methodBody}\n")
    return methodBuilder.toString()
}
