package com.reposilite.shared.extensions

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import org.jetbrains.exposed.sql.statements.api.ExposedConnection
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi

fun ExposedConnection<*>.executeOneOfQueries(
    journalist: Journalist,
    vararg queries: String,
    returnKeys: Boolean = false
): Boolean {
    for (query in queries) {
        var statement: PreparedStatementApi? = null

        try {
            statement = prepareStatement(query, returnKeys)
            statement.executeUpdate()
            return true
        } catch (ignored: Exception) {
            journalist.logger.exception(Channel.DEBUG, ignored)
            continue
        } finally {
            statement?.closeIfPossible()
        }
    }

    return false
}