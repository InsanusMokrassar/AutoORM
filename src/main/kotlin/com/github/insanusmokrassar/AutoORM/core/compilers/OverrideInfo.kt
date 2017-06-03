package com.github.insanusmokrassar.AutoORM.core.compilers

import org.jetbrains.kotlin.com.intellij.util.containers.Stack
import kotlin.reflect.KClass

interface OverrideInfo {
    val nameStack: Stack<String>
    val argsNamesStack: Stack<String>
    val returnClass: KClass<*>

    fun refreshStacks()
}