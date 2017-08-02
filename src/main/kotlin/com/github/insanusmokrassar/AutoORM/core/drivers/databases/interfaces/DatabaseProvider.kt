package com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces

import com.github.insanusmokrassar.AutoORM.core.DatabaseConnect
import com.github.insanusmokrassar.AutoORM.core.generators.RealisationsGenerator
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass

interface DatabaseProvider {
    fun getDatabaseConnect(params: IObject<Any>, classGenerator: RealisationsGenerator, onFreeCallback: (DatabaseConnect) -> Unit, onCloseCallback: (DatabaseConnect) -> Unit) : DatabaseConnect
    fun supportTable(modelClass: KClass<*>) : Boolean
}