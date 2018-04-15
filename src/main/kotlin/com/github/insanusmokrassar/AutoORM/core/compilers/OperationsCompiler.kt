package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import net.openhft.compiler.CompilerUtils
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

private val methodsBodies = mapOf(
        Pair(
                "refresh",
                {
                    method: KFunction<*>, whereFrom: KClass<*> ->
                    val methodBody = StringBuilder()
                    methodBody.append(primaryKeyFindBuilder(whereFrom))
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
        ),
        Pair(
                "update",
                {
                    method: KFunction<*>, whereFrom: KClass<*> ->
                    val methodBody = StringBuilder()
                    methodBody.append(primaryKeyFindBuilder(whereFrom))
                    methodBody.append("$providerVariableName.update(this, $searchQueryName);\n")
                    methodOverrideTemplate(
                            method,
                            methodBody.toString(),
                            whereFrom.isInterface()
                    )
                }
        ),
        Pair(
                "remove",
                {
                    method: KFunction<*>, whereFrom: KClass<*> ->
                    val methodBody = StringBuilder()
                    methodBody.append(primaryKeyFindBuilder(whereFrom))
                    methodBody.append("$providerVariableName.remove($searchQueryName);\n")
                    methodOverrideTemplate(
                            method,
                            methodBody.toString(),
                            whereFrom.isInterface()
                    )
                }
        )
)

class OperationsCompiler(private val compiler: ClassCompiler) {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    @Synchronized
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

        val classBodyBuilder = StringBuilder()
        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")
        variablesToOverride.forEach {
            classBodyBuilder.append("${overrideVariableTemplate(it)}\n")
        }

        classBodyBuilder.append("${createConstructorForProperties(whereFrom, constructorVariables)}\n")

        methodsToOverride.forEach {
            classBodyBuilder.append("${methodsBodies[it.name]!!(it, whereFrom)}\n")
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

        val aClass = compiler.compile<O>(
                interfaceImplementerClassNameTemplate(whereFrom.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName, whereFrom.isInterface())
        )

        return aClass
    }
}
