package com.github.insanusmokrassar.AutoORM.core

import org.jetbrains.kotlin.com.intellij.util.containers.Stack

class ConnectionsPool(createConnectionsCallback: ((DatabaseConnect) -> Unit) -> List<DatabaseConnect>) {
    private val pool = Stack<DatabaseConnect>()
    private val allConnections: MutableList<DatabaseConnect> = ArrayList()
    private val lock = Object()

    private val onFree : (DatabaseConnect) -> Unit = {
        synchronized(lock, {
            if (!pool.contains(it) && allConnections.contains(it)) {
                pool.push(it)
                lock.notify()
            }
        })
    }

    init {
        allConnections.addAll(createConnectionsCallback(onFree))
        pool.addAll(allConnections)
    }

    fun getConnection(): DatabaseConnect {
        synchronized(lock, {
            while (pool.isEmpty()) {
                lock.wait()
            }
            return pool.pop()
        })
    }
}