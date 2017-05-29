package com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts

import com.github.insanusmokrassar.AbstractDatabaseORM.core.*
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.TableProvider
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
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

    val constructorRequiredVariables : List<KProperty<*>> = ArrayList((variablesMap.filter {
        it.value is KMutableProperty<*> || !it.value.isNullable()
    }.values))

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

    protected fun createModelFromValuesMap(values : Map<String, Any>): O {
        val realisationClass = OperationsCompiler.getRealisation(operationsClass).kotlin
        if (realisationClass.constructors.isEmpty()) {
            throw IllegalStateException("For some of reason, can't create correct realisation of model")
        } else {
            val resultModelConstructor = realisationClass.constructors.getFirst {
                TODO("Need to write founding constructor")
            }?:throw IllegalStateException("For some of reason, can't create correct realisation of model")
            val paramsList = ArrayList<Any?>()
            resultModelConstructor.parameters.forEach {
                paramsList.add(
                        values[it.name]
                )
            }
            val result = resultModelConstructor.call(paramsList)
            TODO("Need to write sets for created object")
            return result
        }
    }
}
