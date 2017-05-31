package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces

import com.github.insanusmokrassar.AbstractDatabaseORM.core.DatabaseConnect
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass

interface DatabaseDriver {
    fun getDatabaseConnect(params: IObject<Any>) : DatabaseConnect
    fun supportTable(modelClass: KClass<*>) : Boolean
}