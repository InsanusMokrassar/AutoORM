package com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts.AbstractTableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class JDBCTableProvider<T : Any>(targetClass: KClass<T>) : AbstractTableProvider<T>(targetClass) {
    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun abort() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun submit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(where: SearchQueryCompiler<out Any>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun find(where: SearchQueryCompiler<out Any>): Collection<T> {
        println(where.compileQuery() as String)
        return emptyList()
    }

    override fun getEmptyQuery(): SearchQueryCompiler<out Any> {
        return JDBCSearchQueryCompiler()
    }

    override fun insert(values: Map<KProperty<*>, Any>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(values: Map<KProperty<*>, Any>, where: SearchQueryCompiler<out Any>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}