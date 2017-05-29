package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts

import com.github.insanusmokrassar.AbstractDatabaseORM.core.OperationsCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.getObjectDeclaration
import com.github.insanusmokrassar.AbstractDatabaseORM.core.intsancesKClass
import com.github.insanusmokrassar.AbstractDatabaseORM.core.isNullable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AbstractTableProvider<M : Any, O : M>(protected val modelClass: KClass<M>, protected val operationsClass: KClass<in O>) : TableProvider<M, O> {
    val variablesMap: Map<String, KProperty<*>> = {
        val futureMap = HashMap<String, KProperty<*>>()
        modelClass.members.filter {
            it is KProperty<*>
        }.forEach {
            futureMap.put(it.name, it as KProperty<*>)
        }
        futureMap
    }()

    abstract fun insert(values: Map<KProperty<*>, Any>): Boolean

    override fun insert(what: M): Boolean {
        return insert(toValuesMap(what))
    }

    abstract fun update(values: Map<KProperty<*>, Any>, where: SearchQueryCompiler<out Any>): Boolean

    override fun update(than: M, where: SearchQueryCompiler<out Any>): Boolean {
        return update(toValuesMap(than), where)
    }

    protected fun toValuesMap(what: M) : Map<KProperty<*>, Any> {
        val values = HashMap<KProperty<*>, Any>()

        variablesMap.values.filter {
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

    protected fun createModelFromValuesMap(values : Map<KProperty<*>, Any>): O {
        val realisationClass = OperationsCompiler.getRealisation(operationsClass)
        TODO()
    }

}