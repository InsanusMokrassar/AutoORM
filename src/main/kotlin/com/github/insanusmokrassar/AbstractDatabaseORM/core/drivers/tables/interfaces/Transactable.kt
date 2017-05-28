package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

interface Transactable {
    @Throws(IllegalStateException::class)
    fun start()
    fun abort()
    fun submit()
}