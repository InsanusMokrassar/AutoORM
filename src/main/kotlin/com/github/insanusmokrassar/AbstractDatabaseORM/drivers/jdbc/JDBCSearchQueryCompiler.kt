package com.github.insanusmokrassar.AbstractDatabaseORM.drivers.jdbc

import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.abstracts.AbstractSearchQueryCompiler
import com.github.insanusmokrassar.AbstractDatabaseORM.core.drivers.tables.filters.Filter


private val operations = mapOf(
        Pair(
                "eq",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} != ${it.args[0]}"
                    } else {
                        "${it.field} = ${it.args[0]}"
                    }
                }
        ),
        Pair(
                "gt",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} <= ${it.args[0]}"
                    } else {
                        "${it.field} > ${it.args[0]}"
                    }
                }
        ),
        Pair(
                "gte",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} < ${it.args[0]}"
                    } else {
                        "${it.field} >= ${it.args[0]}"
                    }
                }
        ),
        Pair(
                "lt",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} >= ${it.args[0]}"
                    } else {
                        "${it.field} < ${it.args[0]}"
                    }
                }
        ),
        Pair(
                "lte",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} > ${it.args[0]}"
                    } else {
                        "${it.field} <= ${it.args[0]}"
                    }
                }
        ),
        Pair(
                "in",
                {
                    it: Filter ->
                    if (it.isNot) {
                        "${it.field} < ${it.args[0]} OR ${it.field} > ${it.args[1]}"
                    } else {
                        "${it.field} >= ${it.args[0]} AND ${it.field} <= ${it.args[1]}"
                    }
                }
        ),
        Pair(
                "oneOf",
                {
                    filter: Filter ->
                    val localBuilder = StringBuilder()
                    if (filter.isNot) {
                        filter.args.forEach {
                            localBuilder.append("${filter.field} != $it")
                            if (filter.args.indexOf(it) < filter.args.size - 1) {
                                localBuilder.append(" AND ")
                            }
                        }
                    } else {
                        "${filter.field} >= ${filter.args[0]} AND ${filter.field} <= ${filter.args[1]}"
                    }
                }
        )
)

class JDBCSearchQueryCompiler : AbstractSearchQueryCompiler<String>() {
    override fun compileQuery(): String {
        prepareToCompilingQuery()
        val queryBuilder = StringBuilder()

        filters.forEach {
            if (it.logicalLink != null) {
                queryBuilder.append(
                        " ${it.logicalLink} "
                )
            }
            if (operations.contains(it.filterName)) {
                queryBuilder.append(
                        operations[it.filterName]!!(it)
                )
            } else {
                throw IllegalStateException("Unsupported filter \"${it.filterName}\"")
            }
        }

        return queryBuilder.toString()
    }
}