package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

val nativeTypes = listOf(
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        String::class,
        Boolean::class
)

private val compiledDeclarations = HashMap<KClass<*>, ObjectDeclaration>()

fun getObjectDeclaration(from : KClass<*>) : ObjectDeclaration {
    if (!compiledDeclarations.containsKey(from)) {
        compiledDeclarations.put(from, ObjectDeclaration(from))
    }
    return compiledDeclarations[from]!!
}

class ObjectDeclaration (val source: KClass<*>) {
    val name : String = source.simpleName!!

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

fun KCallable<*>.isReturnNative() : Boolean {
    return nativeTypes.contains(this.returnClass())
}

fun KProperty<*>.isPrimaryField() : Boolean {
    this.annotations.forEach {
        if (it.annotationClass == PrimaryKey::class) {
            return@isPrimaryField true
        }
    }
    return false
}
