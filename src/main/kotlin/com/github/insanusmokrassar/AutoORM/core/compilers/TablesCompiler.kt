package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.SearchQueryCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import java.util.logging.Logger
import kotlin.reflect.*
import kotlin.reflect.full.functions

private val searchQueryName = "searchQuery"
private val resultName = "result"

private val operations = mapOf(
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

private val pagingIdentifiers = mapOf(
        Pair(
                "all",
                {
                    _: KClass<*>, _: OverrideFunctionInfo ->
                    ""
                }
        ),
        Pair(
                "first",
                {
                    _: KClass<*>, _: OverrideFunctionInfo ->
                    "$searchQueryName.paging();\n"
                }
        )
)

private val operationsParamsCounts = mapOf(
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

private val linksWithNextCondition = listOf(
        "or",
        "and"
)

private val whereIdentifiersAlgorithms = mapOf(
        Pair(
                "where",
                {
                    tableInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    constructWhere(tableInterfaceClass, funcInfo)
                }
        ),
        Pair(
                "By",
                {
                    tableInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    constructWhere(tableInterfaceClass, funcInfo)
                }
        )
).plus(pagingIdentifiers)

private fun resultResolver(from: KClass<*>, to: KClass<*>, resultVariable: String = resultName): String {
    when(from) {
        Collection::class -> when(to) {
            List::class -> return "return new ${ArrayList::class.java.simpleName}($resultVariable);\n"
            Boolean::class -> return "return !$resultVariable.isEmpty();\n"
            Unit::class -> return ""
            else -> "return (${to.javaObjectType.simpleName})$resultVariable.toArray()[0];\n"
        }
        to -> return "return $resultVariable;"
        else -> when(to) {
            Unit::class -> return ""
            else -> return "return (${to.javaObjectType.simpleName}) $resultVariable;"
        }
    }
    return ""
}

private fun resultBuilder(tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo, operationName: String): String {
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
                    resultBuilder.append(funcInfo.argsStack.pop().name)
                }
            }
        }
        if (!functionParams.isLast(it)) {
            resultBuilder.append(", ")
        }
    }
    resultBuilder.append(");\n")

    resultBuilder.append(
            resultResolver(
                    TableProvider::class.functions.getFirst { it.name== operationName }!!.returnClass(),
                    funcInfo.returnType.classifier as KClass<*>
            )
    )
    val builder = StringBuilder()
    if (functionParams.select({ it.type.classifier }).contains(SearchQueryCompiler::class)) {
        builder.append("${constructSearchQuery(tableInterfaceClass, modelInterfaceClass, funcInfo)}\n")
    }
    builder.append(resultBuilder)
    return builder.toString()
}

private fun constructWhere(tableInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo): String {
    val builder = StringBuilder()
    while (!funcInfo.nameStack.empty() && !pagingIdentifiers.containsKey(funcInfo.nameStack.peek())) {
        if (linksWithNextCondition.contains(funcInfo.nameStack.peek())) {
            builder.append("$searchQueryName.linkWithNext(\"${funcInfo.nameStack.pop()}\");\n")
        }
        builder.append("$searchQueryName.field(\"${funcInfo.nameStack.pop()}\", false);\n")
        if (funcInfo.nameStack.peek() == "not") {
            builder.append("$searchQueryName.not();\n")
        }

        val filter = funcInfo.nameStack.pop()
        builder.append("$searchQueryName.filter(\"$filter\"")
        try {
            val needParams = operationsParamsCounts[filter]!!
            if (needParams > 0) {
                for (i: Int in 0..needParams - 1) {
                    builder.append(", ${funcInfo.argsStack.pop().name!!}")
                }
            }
            builder.append(");")
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("Invalid query", e)
        }
    }

    if (!funcInfo.nameStack.empty() && pagingIdentifiers.containsKey(funcInfo.nameStack.peek())) {
        builder.append(pagingIdentifiers[funcInfo.nameStack.pop()]!!(tableInterfaceClass, funcInfo))
    }

    return builder.toString()
}

private fun constructSearchQuery(tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo): String {
    val neededFields = HashSet<String>()
    modelInterfaceClass.getRequiredInConstructor().forEach {
        neededFields.add(it.name)
    }
    while (!whereIdentifiersAlgorithms.keys.contains(funcInfo.nameStack.peek())) {
        neededFields.add(funcInfo.nameStack.pop())
    }
    val searchQueryBody = StringBuilder()
    searchQueryBody.append("SearchQueryCompiler $searchQueryName = $providerVariableName.getEmptyQuery();\n")
    if (neededFields.isNotEmpty()) {
        searchQueryBody.append("$searchQueryName.setNeededFields(")
        neededFields.forEach {
            searchQueryBody.append("\"$it\"")
            if (!neededFields.isLast(it)) {
                searchQueryBody.append(", ")
            }
        }
        searchQueryBody.append(");\n")
    }
    try {
        searchQueryBody.append(whereIdentifiersAlgorithms[funcInfo.nameStack.pop()]!!(tableInterfaceClass, funcInfo))
    } catch (e: NullPointerException) {
        Logger.getGlobal().warning("Can't find where identifier for function: ${funcInfo.function.name}; use paging \'all\'")
        searchQueryBody.append(whereIdentifiersAlgorithms["all"]!!(tableInterfaceClass, funcInfo))
    }

    return searchQueryBody.toString()
}

private fun methodBodyBuilder(tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo): String {
    try {
        return resultBuilder(tableInterfaceClass, modelInterfaceClass, funcInfo, operations[funcInfo.nameStack.pop()]!!)
    } catch (e: NullPointerException) {
        throw IllegalStateException("Can't find operation for ${funcInfo.nameParts[0]} in function ${funcInfo.function.name}", e)
    }
}

private class OverrideFunctionInfo(val function: KFunction<*>) {
    val nameParts = function.name.camelCaseWords()
    val args = function.parameters

    val nameStack = Stack<String>()
    val argsStack = Stack<KParameter>()

    val returnType = function.returnType

    init {
        refreshStacks()
    }

    fun refreshStacks() {
        nameStack.clear()
        argsStack.clear()

        nameStack.addAll(nameParts.reversed())
        argsStack.addAll(args.filter {
            it.kind != KParameter.Kind.INSTANCE
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
        addImports(ArrayList::class, headerBuilder)
        addImports(Collection::class, headerBuilder)
        addImports(SearchQueryCompiler::class, headerBuilder)

        methodsToOverride.forEach {
            classBodyBuilder.append(
                    methodOverrideTemplate(it, methodBodyBuilder(tableInterfaceClass, modelInterfaceClass, OverrideFunctionInfo(it)))
            )
        }

        val aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(
                interfaceImplementerClassNameTemplate(tableInterfaceClass.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!, tableInterfaceClass.isInterface())
        )

        return (aClass.kotlin as KClass<out T>)
    }
}
