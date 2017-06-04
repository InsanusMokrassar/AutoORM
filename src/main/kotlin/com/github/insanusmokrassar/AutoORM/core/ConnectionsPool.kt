package com.github.insanusmokrassar.AutoORM.core

import org.jetbrains.kotlin.com.intellij.util.containers.Stack

class ConnectionsPool(
        createConnectionsCallback: ((DatabaseConnect) -> Unit, (DatabaseConnect) -> Unit) -> List<DatabaseConnect>) {
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

    private val onCloseDatabase: (DatabaseConnect) -> Unit = {
        if (pool.contains(it)) {
            pool.remove(it)
        }
        if (allConnections.contains(it)) {
            allConnections.remove(it)
        }
    }

    init {
        allConnections.addAll(createConnectionsCallback(onFree, onCloseDatabase))
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

    fun close() {
        allConnections.forEach {
            it.close()
        }
    }
}