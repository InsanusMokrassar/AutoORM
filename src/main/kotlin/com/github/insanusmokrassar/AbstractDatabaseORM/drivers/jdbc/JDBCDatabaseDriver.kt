package com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc

import com.github.insanusmokrassar.AbstractDatabaseORM.DatabaseConnect
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import java.sql.DriverManager
import kotlin.reflect.KClass

class JDBCDatabaseDriver(parameters: IObject<Any>) : DatabaseDriver {

    init {
        val driver = parameters.get<String>("jdbcDriverPath")
        Class.forName(driver)
    }

    override fun getDatabaseConnect(params: IObject<Any>): DatabaseConnect {
        val connection = DriverManager.getConnection(
                params.get("url"),
                params.get("username"),
                params.get("password")
        )
        return DatabaseConnect(
                JDBCTableDriver(connection),
                JDBCTransactable(connection)
        )
    }

    override fun supportTable(modelClass: KClass<*>): Boolean {
        return true
    }
}