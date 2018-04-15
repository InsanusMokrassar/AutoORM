package com.github.insanusmokrassar.AutoORM.core.compilers

import net.openhft.compiler.CompilerUtils
import kotlin.reflect.KClass

class DefaultClassCompiler: ClassCompiler {
    @Synchronized
    override fun <T: Any> compile(className: String, classCode: String): KClass<T> {
        return (try {
            CompilerUtils.CACHED_COMPILER.loadFromJava(
                    className,
                    classCode
            ).kotlin
        } catch (e: AssertionError) {
            if (e.message ?. contains("duplicate") == true) {
                Class.forName(className).kotlin
            } else {
                throw e
            }
        })  as KClass<T>
    }

}
