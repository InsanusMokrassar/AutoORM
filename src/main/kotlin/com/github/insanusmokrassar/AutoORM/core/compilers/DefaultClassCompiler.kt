package com.github.insanusmokrassar.AutoORM.core.compilers

import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import net.openhft.compiler.CompilerUtils
import kotlin.reflect.KClass

class DefaultClassCompiler: ClassCompiler {
    override fun compile(className: String, classCode: String): kotlin.reflect.KClass<*> {
        return CompilerUtils.CACHED_COMPILER.loadFromJava(
                className,
                classCode
        ).kotlin
    }

}
