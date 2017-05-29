package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter

fun <T>KCallable<T>.intsancesKClass() : KClass<*>{
    return this.instanceParameter?.type?.classifier as KClass<*>
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

inline fun <T> Iterable<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}

inline fun <T> Array<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}

fun String.asSQLString() : String {
    if (this.matches(Regex("^\'.*\'$"))) {
        return this
    } else {
        return "\'$this\'"
    }
}

fun <T>List<T>.elementsIsEqual(other: List<T>) : Boolean {
    this.forEach {
        if (!other.contains(it)) {
            return@elementsIsEqual false
        }
    }
    return true
}
