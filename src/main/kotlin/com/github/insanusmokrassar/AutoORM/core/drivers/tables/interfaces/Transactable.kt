package com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces

interface Transactable {
    @Throws(IllegalStateException::class)
    fun start()
    fun abort()
    fun submit()
}