package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces

interface Transactable {
    fun start()
    fun abort()
    fun submit()
}