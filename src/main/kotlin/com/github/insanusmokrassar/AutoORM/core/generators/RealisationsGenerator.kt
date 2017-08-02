package com.github.insanusmokrassar.AutoORM.core.generators

import kotlin.reflect.KClass

interface RealisationsGenerator {
    fun <T : Any> getTableRealisation(tableInterface: KClass<in T>, modelInterface: KClass<*>) : KClass<out T>
    fun <O : Any> getModelOperationsRealisation(what : KClass<in O>) : KClass<out O>
}