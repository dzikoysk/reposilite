package com.reposilite.shared.extensions

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedConnection
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi

fun ExposedConnection<*>.executeQuery(
    query: String,
    returnKeys: Boolean = false
) {
    var statement: PreparedStatementApi? = null

    try {
        statement = prepareStatement(query, returnKeys)
        statement.executeUpdate()
    } finally {
        statement?.closeIfPossible()
    }
}

fun Op.Companion.andOf(vararg ops: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> {
    require(ops.isNotEmpty()) { "At least one operation required to build 'and' query" }
    var operation: Op<Boolean>? = null
    ops.forEach { operation = operation?.and(it) ?: build(it) }
    return operation!!
}