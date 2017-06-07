package com.github.insanusmokrassar.AutoORM.core.drivers.databases.abstracts

import com.github.insanusmokrassar.AutoORM.core.DatabaseConnect
import com.github.insanusmokrassar.AutoORM.core.drivers.databases.interfaces.DatabaseDriver
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.TableDriver
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces.Transactable
import com.github.insanusmokrassar.iobjectk.interfaces.IObject

abstract class AbstractDatabaseDriver : DatabaseDriver {
    override fun getDatabaseConnect(params: IObject<Any>, onFreeCallback: (DatabaseConnect) -> Unit, onCloseCallback: (DatabaseConnect) -> Unit): DatabaseConnect {
        val driverPartsPair = makeDriverAndTransactable(params)
        return DatabaseConnect(
                this,
                driverPartsPair.first,
                driverPartsPair.second,
                onFreeCallback,
                onCloseCallback
        )
    }

    protected abstract fun makeDriverAndTransactable(params: IObject<Any>): Pair<TableDriver, Transactable>
}