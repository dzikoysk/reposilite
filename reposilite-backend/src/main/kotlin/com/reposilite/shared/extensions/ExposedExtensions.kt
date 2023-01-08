/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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