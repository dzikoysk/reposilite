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

package com.reposilite.shared.extensions

import panda.std.Result
import java.util.concurrent.atomic.AtomicBoolean

internal fun AtomicBoolean.peek(block: () -> Unit) {
    if (this.get()) {
        block()
    }
}

internal fun <T, R> Iterable<T>.firstAndMap(transform: (T) -> R): R? =
    this.firstOrNull()?.let(transform)

internal fun <VALUE, ERROR, MAPPED_ERROR> Sequence<Result<out VALUE, ERROR>>.firstSuccessOr(elseValue: () -> Result<out VALUE, MAPPED_ERROR>): Result<out VALUE, MAPPED_ERROR> =
    this.firstOrNull { it.isOk }
        ?.projectToValue()
        ?: elseValue()

internal fun <VALUE, ERROR> Sequence<Result<out VALUE, ERROR>>.firstOrErrors(): Result<out VALUE, Collection<ERROR>> {
    val collection: MutableCollection<ERROR> = ArrayList()

    return this
        .map { result -> result.onError { collection.add(it) } }
        .firstSuccessOr { Result.error(collection) }
}

internal fun <VALUE> Result<VALUE, *>.`when`(condition: (VALUE) -> Boolean): Boolean =
    fold({ condition(it) }, { false })

internal fun <T> take(condition: Boolean, ifTrue: T, ifFalse: T): T =
    if (condition) ifTrue else ifFalse