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

package com.reposilite.shared

import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T, R> Iterable<T>.firstAndMap(transform: (T) -> R): R? =
    this.firstOrNull()?.let(transform)

suspend fun <T> launchTransaction(dispatcher: CoroutineDispatcher?, database: Database, statement: Transaction.() -> T): T =
    if (dispatcher != null)
        newSuspendedTransaction(dispatcher, database) { statement(this) }
    else
        transaction(database) { statement(this) }