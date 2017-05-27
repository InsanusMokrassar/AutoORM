package com.github.insanusmokrassar.AbstractDatabaseORM.core

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter

fun <T>KProperty<T>.insancesKClass() : KClass<*>{
    return this.instanceParameter?.type?.classifier as KClass<*>
}
