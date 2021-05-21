/*
 * Copyright (c) 2021 dzikoysk
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

package org.panda_lang.reposilite.shared

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager

/*
 * Source: https://github.com/JetBrains/Exposed/issues/167#issuecomment-750333496
 */

fun <T : Table> T.upsert(vararg keys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
    UpsertStatement<Number>(this, keys = keys).apply {
        body(this)
        execute(TransactionManager.current())
    }

fun <T : Table, E> T.batchUpsert(data: Collection<E>, vararg keys: Column<*>, body: T.(BatchUpsertStatement, E) -> Unit) {
    if (data.isEmpty()) {
        return
    }

    BatchUpsertStatement(this, keys = keys).apply {
        data.forEach {
            addBatch()
            body(this, it)
        }
        execute(TransactionManager.current())
    }
}

class UpsertStatement<Key : Any>(table: Table, isIgnore: Boolean = false, private vararg val keys: Column<*>) : InsertStatement<Key>(table, isIgnore) {

    override fun prepareSQL(transaction: Transaction) = buildString {
        append(super.prepareSQL(transaction))
        append(transaction.onUpdateSql(values.keys, *keys))
    }

}

class BatchUpsertStatement(table: Table, isIgnore: Boolean = false, private vararg val keys: Column<*>) : BatchInsertStatement(table, isIgnore) {

    override fun prepareSQL(transaction: Transaction) = buildString {
        append(super.prepareSQL(transaction))
        append(transaction.onUpdateSql(values.keys, *keys))
    }

}

private fun Transaction.onUpdateSql(values: Iterable<Column<*>>, vararg keys: Column<*>) = buildString {
    if (db.isPostgreSQL()) {
        append(" ON CONFLICT (${keys.joinToString(",") { identity(it) }})")
        values
            .filter { it !in keys }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                append(" DO UPDATE SET ")
                fields.joinTo(this, ", ") { "${identity(it)} = EXCLUDED.${identity(it)}" }
            } ?: append(" DO NOTHING")
    } else {
        append(" ON DUPLICATE KEY UPDATE ")
        values.joinTo(this, ", ") { "${identity(it)} = VALUES(${identity(it)})" }
    }
}

fun Database.isPostgreSQL() = vendor == "postgresql"