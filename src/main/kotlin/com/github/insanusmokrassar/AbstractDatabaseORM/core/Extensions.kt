package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter

fun <T>KCallable<T>.intsancesKClass() : KClass<*>{
    return this.instanceParameter?.type?.classifier as KClass<*>
}

inline fun <T> Iterable<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}

inline fun <T> Array<T>.getFirst(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return@getFirst element
    return null
}
