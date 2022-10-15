package com.reposilite.shared.extensions

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