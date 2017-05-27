package com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import kotlin.reflect.KClass

class JDBCDatabaseDriver : DatabaseDriver{
    override fun <T : Any> getProvider(modelClass: KClass<T>): TableProvider<T>? {
        return JDBCTableProvider(modelClass)
    }

    override fun supportTable(modelClass: KClass<*>): Boolean {
        return true
    }
}