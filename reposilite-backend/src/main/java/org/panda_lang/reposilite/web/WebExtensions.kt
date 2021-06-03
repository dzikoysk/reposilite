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

package org.panda_lang.reposilite.web

import io.javalin.http.Context
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.utilities.commons.function.Option
import org.panda_lang.utilities.commons.function.Result

/**
 * Process uri applying following changes:
 *
 *
 *  * Remove root slash
 *  * Remove illegal path modifiers like .. and ~
 *
 *
 * @param uri the uri to process
 * @return the normalized uri
 */
fun normalizeUri(uri: String): Option<String> {
    var normalizedUri = uri

    if (normalizedUri.contains("..") || normalizedUri.contains("~") || normalizedUri.contains(":") || normalizedUri.contains("\\")) {
        return Option.none()
    }

    while (normalizedUri.contains("//")) {
        normalizedUri = normalizedUri.replace("//", "/")
    }

    if (normalizedUri.startsWith("/")) {
        normalizedUri = normalizedUri.substring(1)
    }

    return Option.of(normalizedUri)
}

/**
 * Returns the given string only if the condition is true, otherwise returns empty string
 */
fun textIf(condition: Boolean, value: String): String =
    if (condition) value else ""

/**
 * Extends Javalin's context with a support for [ErrorResponse] results
 */
fun Context.error(error: ErrorResponse): Context =
    this.status(error.status).json(error)

/**
 * Project non-existing value of errored [Result] to simplify error handling by convenient way to match expected signatures.
 * This method throws [IllegalArgumentException] if the given [Result] does not contain error.
 */
fun <ANY_VALUE, REQUIRED_VALUE, ERROR> Result<ANY_VALUE, ERROR>.projectToError(): Result<REQUIRED_VALUE, ERROR> =
    if (this.isErr) this.map { null } else throw IllegalArgumentException("")

/**
 * End [Result] based pipes with
 */
@Suppress("unused")
fun <V, E> Result<V, E>.end(): Unit =
    Unit