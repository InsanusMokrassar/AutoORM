package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.intsancesKClass
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AbstractTableProvider<T : Any>(protected val targetClass : KClass<T>) : TableProvider<T> {

    val variablesList: List<KProperty<*>> = {
        val futureList = ArrayList<KProperty<*>>()
        targetClass.members.filter {
            it is KProperty<*>
        }.forEach {
            futureList.add(it as KProperty<*>)
        }
        futureList
    }()

    abstract fun insert(values: Map<KProperty<*>, Any>): Boolean

    override fun insert(what: T): Boolean {
        return insert(toValuesMap(what))
    }

    abstract fun update(values: Map<KProperty<*>, Any>, where: SearchQueryCompiler<out Any>): Boolean

    override fun update(than: T, where: SearchQueryCompiler<out Any>): Boolean {
        return update(toValuesMap(than), where)
    }

    protected fun toValuesMap(what: T) : Map<KProperty<*>, Any> {
        val values = HashMap<KProperty<*>, Any>()

        variablesList.filter {
            it.intsancesKClass() != Any::class && (!it.returnType.isMarkedNullable || it.call(what) != null)
        }.forEach {
            it.call(what)?.let { value ->
                values.put(
                        it,
                        value
                )
            }
        }
        return values
    }

}