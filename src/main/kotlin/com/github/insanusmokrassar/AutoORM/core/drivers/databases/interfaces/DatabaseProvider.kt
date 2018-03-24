package com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces

import com.github.insanusmokrassar.AutoORM.core.DatabaseConnect
import com.github.insanusmokrassar.AutoORM.core.compilers.ClassCompiler
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import kotlin.reflect.KClass

interface DatabaseProvider {
    fun getDatabaseConnect(
            params: IObject<Any>,
            classCompiler: ClassCompiler,
            onFreeCallback: (DatabaseConnect) -> Unit,
            onCloseCallback: (DatabaseConnect) -> Unit
    ) : DatabaseConnect
    fun supportTable(modelClass: KClass<*>) : Boolean
}