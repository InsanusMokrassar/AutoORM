package com.github.insanusmokrassar.AutoORM.core.generators

import com.github.insanusmokrassar.JavaClassDescriptor.core.ClassDescriptor
import kotlin.reflect.KClass

interface RealisationsCompiler {
    fun compile(classDescriptor: ClassDescriptor) : KClass<*>
}