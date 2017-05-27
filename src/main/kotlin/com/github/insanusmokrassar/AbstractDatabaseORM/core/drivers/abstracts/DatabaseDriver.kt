package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.abstracts

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.interfaces.TableProvider
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import kotlin.reflect.KClass

abstract class DatabaseDriver(val config : IObject<out Any>) {

    abstract fun <T : Any> makeProvider(forWhat : KClass<T>) : TableProvider<T>
}