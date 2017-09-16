package com.github.insanusmokrassar.AutoORM.core.drivers.databases.abstracts

import com.github.insanusmokrassar.AutoORM.core.DatabaseConnect
import com.github.insanusmokrassar.AutoORM.core.compilers.ClassCompiler
import com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces.DatabaseProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.ConnectionProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.Transactable
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

abstract class AbstractDatabaseProvider : DatabaseProvider {
    override fun getDatabaseConnect(params: IObject<Any>, classCompiler: ClassCompiler, onFreeCallback: (DatabaseConnect) -> Unit, onCloseCallback: (DatabaseConnect) -> Unit): DatabaseConnect {
        val driverPartsPair = makeDriverAndTransactable(params)
        return DatabaseConnect(
                classCompiler,
                driverPartsPair.first,
                driverPartsPair.second,
                onFreeCallback,
                onCloseCallback
        )
    }

    protected abstract fun makeDriverAndTransactable(params: IObject<Any>): Pair<ConnectionProvider, Transactable>
}