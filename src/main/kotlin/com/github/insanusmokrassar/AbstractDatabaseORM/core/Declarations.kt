package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter

private val compiledDeclarations = HashMap<KClass<*>, ObjectDeclaration>()

fun getObjectDeclaration(from : KClass<*>) : ObjectDeclaration {
    if (!compiledDeclarations.containsKey(from)) {
        compiledDeclarations.put(from, ObjectDeclaration(from))
    }
    return compiledDeclarations[from]!!
}

class ObjectDeclaration (val source: KClass<*>) {
    val fields : List<KProperty<*>> = {
        val toReturn = ArrayList<KProperty<*>>()
        source.members.filter {
            it.isField()
        }.forEach {
            toReturn.add(it as KProperty<*>)
        }
        toReturn
    }()

    val functions : List<KFunction<*>> = {
        val toReturn = ArrayList<KFunction<*>>()
        source.members.filter {
            it.isFunction()
        }.forEach {
            toReturn.add(it as KFunction<*>)
        }
        toReturn
    }()

    val notAnyFunctions : List<KFunction<*>> = {
        val toReturn = ArrayList<KFunction<*>>()
        functions.filter {
            it.intsancesKClass() != Any::class
        }.forEach {
            toReturn.add(it as KFunction<*>)
        }
        toReturn
    }()
}

fun KCallable<*>.isNullable() : Boolean {
    return this.returnType.isMarkedNullable
}

fun KCallable<*>.returnClass() : KClass<*> {
    return this.returnType.classifier as KClass<*>
}

fun <T>KCallable<T>.isField() : Boolean {
    return this is KProperty<T>
}

fun <T>KCallable<T>.isFunction() : Boolean {
    return this is KFunction<T>
}