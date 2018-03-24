package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.SearchQuery
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.Filter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.filters.PageFilter
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*
import java.util.logging.Logger
import kotlin.reflect.*
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaSetter

val providerVariableName = "provider"

val searchQueryName = "searchQuery"
val filterName = "filter"
val pageFilterName = "pageFilter"
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
                    "$searchQueryName.setPageFilter(new ${PageFilter::class.simpleName}());\n"
                }
        ),
        Pair(
                "on",
                {
                    it: OverrideInfo ->
                    val pageFilterBuilder = StringBuilder()
                    pageFilterBuilder.append("${PageFilter::class.simpleName} $pageFilterName = new ${PageFilter::class.simpleName}();\n")
                    pageFilterBuilder.append("$pageFilterName.${(PageFilter::class.getVariables().first { it.name == "page" } as KMutableProperty<*>).javaSetter!!.name}(${it.argsNamesStack.pop()});\n")
                    pageFilterBuilder.append("$pageFilterName.${(PageFilter::class.getVariables().first { it.name == "size" } as KMutableProperty<*>).javaSetter!!.name}(${it.argsNamesStack.pop()});\n")
                    pageFilterBuilder.append("$searchQueryName.setPageFilter($pageFilterName);\n")
                    pageFilterBuilder.toString()
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
                "oneof",
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
                "by",
                {
                    funcInfo: OverrideInfo ->
                    constructBy(funcInfo)
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
    if (from.type != null) {
        val currentImport = importTemplate((from.type as KClass<*>).javaObjectType.canonicalName)
        if (!to.contains(currentImport)) {
            to.append("$currentImport\n")
        }
        from.type?.arguments?.forEach {
            addImports(it, to)
        }
    }
}


fun constructWhere(funcInfo: OverrideInfo): String {
    val builder = StringBuilder()
    val filters = ArrayList<String>()
    while (funcInfo.nameStack.isNotEmpty()
            && !(
                    funcInfo.nameStack.size == 1 &&
                            pagingIdentifiers.containsKey(
                                    funcInfo.nameStack.peek()
                            )
                    )
    ) {
        val currentFilterBuilder = StringBuilder()
        if (filters.isEmpty()) {
            currentFilterBuilder.append("${Filter::class.simpleName} $filterName;")
        }
        var fieldName: String = funcInfo.nameStack.pop()
        while (funcInfo.nameStack.isNotEmpty()
                && !filtersArgsCounts.keys.contains(funcInfo.nameStack.peek())
                && !whereIdentifiersAlgorithms.keys.contains(funcInfo.nameStack.peek())
                && !linksWithNextCondition.contains(funcInfo.nameStack.peek())) {
            fieldName += funcInfo.nameStack.pop()
        }
        var filterOrOutField = if (funcInfo.nameStack.isEmpty() || !filtersArgsCounts.keys.contains(funcInfo.nameStack.peek())) {
            "is"
        } else {
            funcInfo.nameStack.pop()
        }
        val isOut = !filtersArgsCounts.keys.contains(filterOrOutField) && filterOrOutField != "not"
        currentFilterBuilder.append(
                "$filterName = new ${Filter::class.simpleName}(\"$fieldName\", $isOut);\n"
        )
        if (isOut) {
            filterOrOutField = funcInfo.nameStack.pop()
        } else if (filterOrOutField == "not") {
            filterOrOutField = funcInfo.nameStack.pop()
            currentFilterBuilder.append("$filterName.setNot(true);\n")
        }
        currentFilterBuilder.append("$filterName.setFilterName(\"$filterOrOutField\");\n")
        for (i: Int in 0 until filtersArgsCounts[filterOrOutField]!!) {
            val arg = funcInfo.argsNamesStack.pop()
            val param = funcInfo.function?.parameters?.first { it.name == arg }
            if (param?.type != null && (param.type.classifier as KClass<*>).isSubclassOf(Collection::class)) {
                currentFilterBuilder.append("$filterName.getArgs().addAll($arg);\n")
            } else {
                currentFilterBuilder.append("$filterName.getArgs().add($arg);\n")
            }
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

fun constructBy(funcInfo: OverrideInfo): String {
    funcInfo.argsNamesStack
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
    searchQueryBody.append("${SearchQuery::class.simpleName} $searchQueryName = new ${SearchQuery::class.simpleName}();\n")
    neededFields.forEach {
        searchQueryBody.append("$searchQueryName.getFields().add(\"$it\");\n")
    }
    try {
        searchQueryBody.append(whereIdentifiersAlgorithms[funcInfo.nameStack.pop()]!!(funcInfo))
    } catch (e: NullPointerException) {
        Logger.getGlobal().warning("Can't find where identifier. Use paging \'all\'")
        searchQueryBody.append(whereIdentifiersAlgorithms["all"]!!(funcInfo))
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
    return when(from) {
        Collection::class -> when(to) {
            List::class -> "new ${ArrayList::class.java.simpleName}($resultVariable)"
            Boolean::class -> "!$resultVariable.isEmpty()"
            Unit::class -> ""
            else -> "(${to.javaObjectType.simpleName})$resultVariable.toArray()[0]"
        }
        to -> resultVariable
        else -> when(to) {
            Unit::class -> ""
            else -> "(${to.javaObjectType.simpleName}) $resultVariable"
        }
    }
}

fun functionCodeBuilder(modelInterfaceClass: KClass<*>, funcInfo: OverrideInfo, operationName: String): String {
    val currentFunction = TableProvider::class.functions.first { it.name== operationName }!!
    val functionParams = currentFunction.parameters.filter { it.kind != KParameter.Kind.INSTANCE }
    val resultBuilder = StringBuilder()
    resultBuilder.append("${TableProvider::class.functions.first { it.name== operationName }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.$operationName(")
    functionParams.forEach {
        val classifier = it.type.classifier
        when(classifier) {
            SearchQuery::class -> resultBuilder.append(searchQueryName)
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
            TableProvider::class.functions.first { it.name== operationName }!!.returnClass(),
            funcInfo.returnClass
    )
    if (resolvedResult.isNotEmpty()) {
        resultBuilder.append(
            "return $resolvedResult;\n"
        )
    }
    val builder = StringBuilder()
    if (functionParams.select({ it.type.classifier }).contains(SearchQuery::class)) {
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

fun addStandardImports(headerBuilder: StringBuilder) {
    addImports(Filter::class, headerBuilder)
    addImports(PageFilter::class, headerBuilder)
    addImports(ArrayList::class, headerBuilder)
    addImports(Arrays::class, headerBuilder)
    addImports(Collection::class, headerBuilder)
    addImports(SearchQuery::class, headerBuilder)
}

fun getterTemplate(fieldProperty: KProperty<*>): String {
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

fun setterTemplate(fieldProperty: KProperty<*>): String {
    val overrideBuilder = StringBuilder()
    if (!fieldProperty.isNullable()) {
        overrideBuilder.append("@${NotNull::class.simpleName}\n")
    }
    overrideBuilder
            .append("public void set${fieldProperty.name[0].toUpperCase()}${fieldProperty.name.substring(1)}(${fieldProperty.toJavaPropertyString()} ${fieldProperty.name}) {\n")
            .append("    this.${fieldProperty.name} = ${fieldProperty.name};\n}")
    return overrideBuilder.toString()
}

fun overrideVariableTemplate(fieldProperty: KProperty<*>): String {
    val overrideBuilder = StringBuilder()
    overrideBuilder.append("${privateFieldTemplate(fieldProperty)}\n\n${getterTemplate(fieldProperty)}\n")

    if (fieldProperty.isMutable()) {
        overrideBuilder.append(setterTemplate(fieldProperty))
    }

    return overrideBuilder.toString()
}

fun primaryKeyFindBuilder(whereFrom: KClass<*>, primaryFields: List<KCallable<*>> = whereFrom.getPrimaryFields()): String {
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
    return constructSearchQuery(
            whereFrom,
            OverridePseudoInfo(namesStack, argsStack)
    )
}

class OverridePseudoInfo(
        val presetNames: Stack<String>,
        val presetArgsNames: Stack<String>,
        override val function: KFunction<*>? = null,
        override val returnClass: KClass<*> = Unit::class): OverrideInfo {
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
