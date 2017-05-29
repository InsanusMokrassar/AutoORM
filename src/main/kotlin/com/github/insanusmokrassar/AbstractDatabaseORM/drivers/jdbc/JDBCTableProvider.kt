package com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc

import com.github.insanusmokrassar.AbstractDatabaseORM.core.*
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts.AbstractTableProvider
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.interfaces.SearchQueryCompiler
import java.sql.Connection
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

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

    init {
//        val checkStatement = connection.prepareStatement("SELECT * FROM information_schema.tables WHERE table_name='${modelClass.simpleName}';")
//        val resultSet = checkStatement.executeQuery()
//        if (resultSet.next()) {
//            TODO()
//        } else {
        val declaration = getObjectDeclaration(modelClass)

        val fieldsBuilder = StringBuilder()
        val primaryField = declaration.fields.getFirst {
            it.isPrimaryField()
        }
        declaration.fields.forEach {
            if (it.isReturnNative()) {
                fieldsBuilder.append("${it.name} ${nativeTypesMap[it.returnClass()]}")
                if (!it.isNullable()) {
                    fieldsBuilder.append(" NOT NULL")
                }
                if (it == primaryField && primaryField.returnClass().isSubclassOf(Number::class)) {
                    fieldsBuilder.append(" AUTO_INCREMENT")
                }
            } else {
                TODO()
            }
            fieldsBuilder.append(", ")
        }
        primaryField?.let {
            fieldsBuilder.append("PRIMARY KEY (${primaryField.name})")
        }

        try {
            if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS ${declaration.name} ($fieldsBuilder);").execute()) {
                Logger.getGlobal().info("Table ${declaration.name} was created")
            }
        } catch (e: Exception) {
            Logger.getGlobal().throwing(this::class.simpleName, "init", e)
            throw IllegalArgumentException("Can't create table ${declaration.name}", e)
        }
//        val lastCheckStatement = connection.prepareStatement("SELECT * FROM information_schema.tables WHERE table_name='${modelClass.simpleName}';")
//        val lastResultSet = lastCheckStatement.executeQuery()
//        if (!lastResultSet.next()) {
//            throw IllegalStateException("For some of reason I can't create table")
//        }
//        }
    }

    override fun remove(where: SearchQueryCompiler<out Any>): Boolean {
        if (where is JDBCSearchQueryCompiler) {
            val queryBuilder = StringBuilder().append("DELETE FROM ${modelClass.simpleName}${where.compileQuery()}${where.compilePaging()};")
            val statement = connection.prepareStatement(queryBuilder.toString())
            return statement.execute()
        } else{
            throw IllegalArgumentException("JDBC provider can't handle query compiler of other providers")
        }
    }

    override fun find(where: SearchQueryCompiler<out Any>): Collection<O> {
        if (where is JDBCSearchQueryCompiler) {
            val queryBuilder = StringBuilder().append("SELECT")
            if (where.getFields == null) {
                queryBuilder.append(" * ")
            } else {
                where.getFields!!.forEach {
                    queryBuilder.append(" $it")
                    if (where.getFields!!.indexOf(it) < where.getFields!!.size - 1) {
                        queryBuilder.append(",")
                    }
                }
            }
            queryBuilder.append("${where.compileQuery()}${where.compilePaging()};")

            val resultSet = connection.prepareStatement(queryBuilder.toString()).executeQuery()
            val result = ArrayList<O>()
            while (resultSet.next()) {
                val currentValuesMap = HashMap<KProperty<*>, Any>()
                variablesList.forEach {
                    currentValuesMap.put(it, resultSet.getObject(it.name, it.javaClass))
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
        val queryBuilder = StringBuilder().append("INSERT INTO ${modelClass.simpleName} SET ")
        values.forEach {
            if (it.value is String) {
                queryBuilder.append(" ${it.key.name} = ${(it.value as String).asSubstring()}")
            } else {
                queryBuilder.append(" ${it.key.name} = ${it.value}")
            }
            if (values.keys.indexOf(it.key) < values.size - 1) {
                queryBuilder.append(",")
            }
        }
        queryBuilder.append(";")
        val statement = connection.prepareStatement(queryBuilder.toString())
        return statement.execute()
    }

    override fun update(values: Map<KProperty<*>, Any>, where: SearchQueryCompiler<out Any>): Boolean {
        if (where is JDBCSearchQueryCompiler) {
            val queryBuilder = StringBuilder().append("UPDATE ${modelClass.simpleName} SET ")
            values.forEach {
                queryBuilder.append(" ${it.key.name} = ${it.value}")
            }
            queryBuilder.append("${where.compileQuery()}${where.compilePaging()};")
            val statement = connection.prepareStatement(queryBuilder.toString())
            return statement.execute()
        } else {
            throw IllegalArgumentException("JDBC provider can't handle query compiler of other providers")
        }
    }
}
