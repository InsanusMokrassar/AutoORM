package com.github.insanusmokrassar.AutoORM.drivers.jdbc

import com.github.insanusmokrassar.AutoORM.core.*
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts.AbstractTableProvider
import com.github.insanusmokrassar.AutoORM.core.drivers.tables.abstracts.SearchQueryCompiler
import java.sql.Connection
import java.util.logging.Logger
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

val nativeTypesMap = mapOf(
        Pair(
                Int::class,
                "INTEGER"
        ),
        Pair(
                Long::class,
                "LONG"
        ),
        Pair(
                Float::class,
                "FLOAT"
        ),
        Pair(
                Double::class,
                "DOUBLE"
        ),
        Pair(
                String::class,
                "TEXT"
        ),
        Pair(
                Boolean::class,
                "BOOLEAN"
        )
)

class JDBCTableProvider<M : Any, O : M>(
        modelClass: KClass<M>,
        operationsClass: KClass<in O>,
        val connection: Connection)
    : AbstractTableProvider<M, O>(
        modelClass,
        operationsClass) {

    protected val primaryFields: List<KCallable<*>>

    init {
        val declaration = getObjectDeclaration(modelClass)

        val fieldsBuilder = StringBuilder()
        primaryFields = declaration.fields.filter {
            it.isPrimaryField()
        }
        declaration.fields.forEach {
            if (it.isReturnNative()) {
                fieldsBuilder.append("${it.name} ${nativeTypesMap[it.returnClass()]}")
                if (!it.isNullable()) {
                    fieldsBuilder.append(" NOT NULL")
                }
                if (primaryFields.contains(it) && it.isAutoincrement()) {
                    fieldsBuilder.append(" AUTO_INCREMENT")
                }
            } else {
                TODO()
            }
            fieldsBuilder.append(", ")
        }
        if (primaryFields.isNotEmpty()) {
            fieldsBuilder.append("CONSTRAINT ${modelClass.simpleName}_PR_KEY PRIMARY KEY (")
            primaryFields.forEach {
                fieldsBuilder.append(it.name)
                if (!primaryFields.isLast(it)) {
                    fieldsBuilder.append(", ")
                }
            }
            fieldsBuilder.append(")")
        }

        try {
            if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS ${declaration.name} ($fieldsBuilder);").execute()) {
                Logger.getGlobal().info("Table ${declaration.name} was created")
            }
        } catch (e: Exception) {
            Logger.getGlobal().throwing(this::class.simpleName, "init", e)
            throw IllegalArgumentException("Can't create table ${declaration.name}", e)
        }
    }

    override fun remove(where: SearchQueryCompiler<out Any>): Boolean {
        if (where is JDBCSearchQueryCompiler) {
            val queryBuilder = StringBuilder().append("DELETE FROM ${modelClass.simpleName} ${where.compileQuery()}${where.compilePaging()};")
            val statement = connection.prepareStatement(queryBuilder.toString())
            return statement.execute()
        } else{
            throw IllegalArgumentException("JDBC provider can't handle query compiler of other providers")
        }
    }

    override fun find(where: SearchQueryCompiler<out Any>): Collection<O> {
        if (where is JDBCSearchQueryCompiler) {
            checkSearchCompileQuery(where)
            val queryBuilder = StringBuilder().append("SELECT ")
            if (where.fields == null) {
                queryBuilder.append("* ")
            } else {
                where.fields!!.forEach {
                    queryBuilder.append(it)
                    if (where.fields!!.indexOf(it) < where.fields!!.size - 1) {
                        queryBuilder.append(",")
                    }
                }
            }
            queryBuilder.append(" FROM ${modelClass.simpleName} ${where.compileQuery()}${where.compilePaging()};")

            val resultSet = connection.prepareStatement(queryBuilder.toString()).executeQuery()
            val result = ArrayList<O>()
            while (resultSet.next()) {
                val currentValuesMap = HashMap<KProperty<*>, Any>()
                if (where.fields == null) {
                    variablesMap.values.forEach {
                        currentValuesMap.put(it, resultSet.getObject(it.name, it.returnClass().java))
                    }
                } else {
                    where.fields!!.forEach {
                        val currentProperty = variablesMap[it]!!
                        currentValuesMap.put(currentProperty, resultSet.getObject(it, currentProperty.returnClass().javaObjectType))
                    }
                }
                result.add(createModelFromValuesMap(currentValuesMap))
            }
            return result
        } else {
            throw IllegalArgumentException("JDBC provider can't handle query compiler of other providers")
        }
    }

    override fun getEmptyQuery(): SearchQueryCompiler<out Any> {
        return JDBCSearchQueryCompiler()
    }

    override fun insert(values: Map<KProperty<*>, Any>): Boolean {
        val queryBuilder = StringBuilder().append("INSERT INTO ${modelClass.simpleName}")
        val fieldsBuilder = StringBuilder()
        val valuesBuilder = StringBuilder()
        val valuesList = values.toList()
        valuesList.forEach {
            fieldsBuilder.append(it.first.name)
            if (it.second is String) {
                valuesBuilder.append((it.second as String).asSQLString())
            } else {
                valuesBuilder.append(it.second.toString())
            }
            if (valuesList.indexOf(it) < valuesList.size - 1) {
                fieldsBuilder.append(",")
                valuesBuilder.append(",")
            }
        }
        queryBuilder.append(" ($fieldsBuilder) VALUES ($valuesBuilder);")
        val statement = connection.prepareStatement(queryBuilder.toString())
        return statement.execute()
    }

    override fun update(values: Map<KProperty<*>, Any>, where: SearchQueryCompiler<out Any>): Boolean {
        if (where is JDBCSearchQueryCompiler) {
            val queryBuilder = StringBuilder().append("UPDATE ${modelClass.simpleName} SET ")
            values.forEach {
                if (it.value is String) {
                    queryBuilder.append(" ${it.key.name}=\'${it.value}\'")
                } else {
                    queryBuilder.append(" ${it.key.name}=${it.value}")
                }
                if (!values.keys.isLast(it.key)) {
                    queryBuilder.append(",")
                }
            }
            queryBuilder.append("${where.compileQuery()}${where.compilePaging()};")
            val statement = connection.prepareStatement(queryBuilder.toString())
            return statement.execute()
        } else {
            throw IllegalArgumentException("JDBC provider can't handle query compiler of other providers")
        }
    }

    protected fun checkSearchCompileQuery(query : JDBCSearchQueryCompiler) {
        if (primaryFields.isNotEmpty() && !query.fields.containsAll(primaryFields.select({it.name}))) {
            query.fields.addAll(primaryFields.select({it.name}))
        }
    }
}
