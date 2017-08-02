package com.github.insanusmokrassar.AutoORM.core.drivers.databases.abstracts

import com.github.insanusmokrassar.AutoORM.core.DatabaseConnect
import com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces.DatabaseProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.ConnectionProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.Transactable
import com.github.insanusmokrassar.AutoORM.core.generators.RealisationsGenerator
import com.github.insanusmokrassar.iobjectk.interfaces.IObject

abstract class AbstractDatabaseProvider : DatabaseProvider {
    override fun getDatabaseConnect(params: IObject<Any>, generator: RealisationsGenerator, onFreeCallback: (DatabaseConnect) -> Unit, onCloseCallback: (DatabaseConnect) -> Unit): DatabaseConnect {
        val driverPartsPair = makeDriverAndTransactable(params)
        return DatabaseConnect(
                generator,
                driverPartsPair.first,
                driverPartsPair.second,
                onFreeCallback,
                onCloseCallback
        )
    }

    protected abstract fun makeDriverAndTransactable(params: IObject<Any>): Pair<ConnectionProvider, Transactable>
}