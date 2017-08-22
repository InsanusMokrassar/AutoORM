package com.github.insanusmokrassar.AutoORM.core.compilers

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface OverrideInfo {
    val nameStack: Stack<String>
    val argsNamesStack: Stack<String>
    val returnClass: KClass<*>
    val function: KFunction<*>?

    fun refreshStacks()
}