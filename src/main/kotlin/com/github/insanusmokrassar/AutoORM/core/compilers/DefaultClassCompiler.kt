package com.github.insanusmokrassar.AutoORM.core.compilers

import net.openhft.compiler.CompilerUtils
import kotlin.reflect.KClass

class DefaultClassCompiler: ClassCompiler {
    override fun <T: Any> compile(className: String, classCode: String): KClass<T> {
        return CompilerUtils.CACHED_COMPILER.loadFromJava(
                className,
                classCode
        ).kotlin as KClass<T>
    }

}
