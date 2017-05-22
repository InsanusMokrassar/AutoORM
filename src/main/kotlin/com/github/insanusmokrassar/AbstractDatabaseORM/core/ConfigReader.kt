package com.github.insanusmokrassar.AbstractDatabaseORM.core

import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import java.util.logging.Logger

class ConfigReader(val params : IObject<Any>) {

    protected val driversConfigs : Map<String, IObject<Any>>

    init {
        val driversRedact = HashMap<String, IObject<Any>>()

        val driversConfigsList : List<IObject<Any>> = params.get("drivers")

        for (driverConfig in driversConfigsList) {
            driversRedact.put(
                    driverConfig.get("name"),
                    driverConfig
            )
        }

        driversConfigs = driversRedact

        Logger.getGlobal().info("Config reader loaded with:\nDrivers configs:\n{$driversConfigs}")
    }
}