package com.github.insanusmokrassar.AutoORM.core.compilers

import kotlin.reflect.KClass

interface ClassCompiler {
    fun <T : Any> compile(className: String, classCode: String): KClass<T>
}