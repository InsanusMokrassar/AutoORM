package com.github.insanusmokrassar.AutoORM.core.drivers.tables.interfaces

import com.github.insanusmokrassar.AutoORM.core.drivers.tables.SearchQuery

interface TableProvider<M : Any, out O : M> {
    fun insert(what : M) : Boolean
    fun update(than : M, where : SearchQuery) : Boolean
    fun remove(where : SearchQuery) : Boolean
    fun find(where : SearchQuery) : Collection<O>
}