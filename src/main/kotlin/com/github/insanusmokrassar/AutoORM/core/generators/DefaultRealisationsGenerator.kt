package com.github.insanusmokrassar.AutoORM.core.generators

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.JavaClassDescriptor.core.FieldDescriptor
import com.github.insanusmokrassar.JavaClassDescriptor.core.getDefaultDescriptor
import com.github.insanusmokrassar.JavaClassDescriptor.realisation.DefaultFieldDescriptor
import com.github.insanusmokrassar.JavaClassDescriptor.realisation.calls.EquateCallingDescriptor
import com.github.insanusmokrassar.JavaClassDescriptor.realisation.calls.FieldCallingDescriptor
import net.openhft.compiler.CompilerUtils
import java.lang.reflect.Modifier
import java.util.HashMap
import kotlin.reflect.KClass

class DefaultRealisationsGenerator(val compiler: RealisationsCompiler) {
    private val compiledMap : MutableMap<KClass<out Any>, KClass<out Any>> = HashMap()

    override fun <T : Any> getTableRealisation(tableInterface: KClass<in T>, modelInterface: KClass<*>) : KClass<T> {
        if (!compiledMap.containsKey(tableInterface)) {
            compiledMap.put(tableInterface, compile(tableInterface, modelInterface))
        }
        return compiledMap[tableInterface] as KClass<T>
    }

    private fun <T : Any> compile(tableInterfaceClass: KClass<in T>, modelInterfaceClass: KClass<*>) : KClass<out T> {
        if (!tableInterfaceClass.isAbstract || !tableInterfaceClass.constructors.isEmpty()) {
            throw IllegalArgumentException("Can't override not interface")
        }
        val methodsToOverride = tableInterfaceClass.java.getMethodsToOverride()

        val classDescription = getDefaultDescriptor("${tableInterfaceClass.simpleName}Impl", Modifier.PUBLIC)

        val tableProviderVarDescription = classDescription.addField(
                providerVariableName,
                TableProvider::class.java,
                Modifier.PRIVATE
        )

        val constructor = classDescription.addConstructor(
                arrayOf(TableProvider::class.java),
                Modifier.PUBLIC
        )
        constructor.addCalling(
                EquateCallingDescriptor(
                        tableProviderVarDescription,
                        FieldCallingDescriptor(
                                constructor.fields.first { it.fieldClass == TableProvider::class.java }
                        )
                )
        )
        val classBodyBuilder = StringBuilder()
//        classBodyBuilder.append("${privateFieldTemplate(TableProvider::class, providerVariableName)}\n")

//        classBodyBuilder.append("${createConstructorForProperties(tableInterfaceClass, emptyList())}\n")

        preparedImportsClasses.forEach {
            classDescription.addImport(it.java)
        }

//        val headerBuilder = StringBuilder()
//                .append("${packageTemplate(tableInterfaceClass.getPackage())}\n")
//                .append(preparedImports)
//        methodsToOverride.forEach {
//            addImports(it, headerBuilder)
//        }
//        addStandardImports(headerBuilder)

        methodsToOverride.forEach {
//            val funcInfo = OverrideFunctionInfo(it)
            val method = classDescription.addMethod(
                    it.name,
                    it.returnType,
                    it.parameterTypes,
                    it.modifiers
            )

            functionCodeBuilder(
                    classDescription,
                    method
            )
//            classBodyBuilder.append(
//                    methodOverrideTemplate(
//                            it,
//                            functionCodeBuilder(modelInterfaceClass, funcInfo, operations[funcInfo.nameStack.pop()]!!),
//                            tableInterfaceClass.isInterface()
//                    )
//            )

        }

        methodsToOverride.forEach {
            val funcInfo = OverrideFunctionInfo(it)
            classBodyBuilder.append(
                    methodOverrideTemplate(
                            it,
                            functionCodeBuilder(modelInterfaceClass, funcInfo, operations[funcInfo.nameStack.pop()]!!),
                            tableInterfaceClass.isInterface()
                    )
            )
        }

        val aClass = compile(
                interfaceImplementerClassNameTemplate(tableInterfaceClass.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), tableInterfaceClass.simpleName!!, tableInterfaceClass.isInterface())
        )

        return aClass as KClass<out T>
    }

    override fun <O : Any> getModelOperationsRealisation(what: KClass<in O>): KClass<out O> {
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
            classBodyBuilder.append("${operationsMethodsBodies[it.name]!!(it, whereFrom)}\n")
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

        val aClass = compile(
                interfaceImplementerClassNameTemplate(whereFrom.java.canonicalName),
                classImplementerTemplate(headerBuilder.toString(), classBodyBuilder.toString(), whereFrom.java.simpleName, whereFrom.isInterface()))

        return aClass as KClass<out O>
    }

    fun compile(className: String, classCode: String) {
        CompilerUtils.CACHED_COMPILER.loadFromJava(
                className,
                classCode
        ).kotlin
    }
}