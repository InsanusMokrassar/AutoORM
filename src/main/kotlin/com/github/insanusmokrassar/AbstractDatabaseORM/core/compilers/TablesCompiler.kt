package com.github.insanusmokrassar.AbstractDatabaseORM.core.compilers

import com.github.insanusmokrassar.AbstractDatabaseORM.core.*
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
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

private val resultBuilders = mapOf(
        Pair(
                "find",
                {
                    tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    val builder = StringBuilder()
                    builder.append("${constructSearchQuery(tableInterfaceClass, modelInterfaceClass, funcInfo)}\n")
                    builder.append("${TableProvider::class.functions.getFirst { it.name=="find" }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.find($searchQueryName);\n")
                    if (funcInfo.returnType.classifier as KClass<*> == List::class) {
                        builder.append("return new ${ArrayList::class.java.simpleName}($resultName);\n")
                    } else {
                        if (funcInfo.returnType.classifier as KClass<*> != Unit::class && TableProvider::class.functions.getFirst { it.name=="find" }!!.returnClass() != funcInfo.returnType.classifier) {
                            builder.append("return (${(funcInfo.returnType.classifier as KClass<*>).javaObjectType})$resultName.toArray()[0];\n")
                        }
                        if (TableProvider::class.functions.getFirst { it.name=="find" }!!.returnClass() != funcInfo.returnType.classifier) {
                            builder.append("return $resultName;\n")
                        }
                    }
                    builder.toString()
                }
        ),
        Pair(
                "update",
                {
                    tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    val builder = StringBuilder()
                    val objectArg = funcInfo.argsStack.pop()
                    builder.append("${constructSearchQuery(tableInterfaceClass, modelInterfaceClass, funcInfo)}\n")
                    builder.append("${TableProvider::class.functions.getFirst { it.name=="update" }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.update(${objectArg.name}, $searchQueryName);\n")
                    if (TableProvider::class.functions.getFirst { it.name=="update" }!!.returnClass() == funcInfo.returnType.classifier) {
                        builder.append("return $resultName;\n")
                    }
                    builder.toString()
                }
        ),
        Pair(
                "insert",
                {
                    _: KClass<*>, _: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    val builder = StringBuilder()
                    builder.append("${TableProvider::class.functions.getFirst { it.name=="insert" }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.insert(${funcInfo.argsStack.pop().name});\n")
                    if (TableProvider::class.functions.getFirst { it.name=="insert" }!!.returnClass() == funcInfo.returnType.classifier) {
                        builder.append("return $resultName;\n")
                    }
                    builder.toString()
                }
        ),
        Pair(
                "remove",
                {
                    tableInterfaceClass: KClass<*>, modelInterfaceClass: KClass<*>, funcInfo: OverrideFunctionInfo ->
                    val builder = StringBuilder()
                    builder.append("${constructSearchQuery(tableInterfaceClass, modelInterfaceClass, funcInfo)}\n")
                    builder.append("${TableProvider::class.functions.getFirst { it.name=="remove" }!!.returnType.toJavaPropertyString()} $resultName = $providerVariableName.remove($searchQueryName);\n")
                    if (TableProvider::class.functions.getFirst { it.name=="remove" }!!.returnClass() == funcInfo.returnType.classifier) {
                        builder.append("return $resultName;\n")
                    }
                    builder.toString()
                }
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

                    builder.toString()
                }
        )
).plus(pagingIdentifiers)

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
        return resultBuilders[operations[funcInfo.nameStack.pop()]]!!(tableInterfaceClass, modelInterfaceClass, funcInfo)
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
                classImplementatorTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!)
        )

        return (aClass.kotlin as KClass<out T>)
    }
}
