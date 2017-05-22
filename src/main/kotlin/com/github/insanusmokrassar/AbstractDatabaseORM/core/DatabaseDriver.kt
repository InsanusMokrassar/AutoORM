package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import java.util.logging.Logger

abstract class DatabaseDriver(parameters: Any) {
    init {
        Logger.getGlobal().info(parameters.toString())
    }
}