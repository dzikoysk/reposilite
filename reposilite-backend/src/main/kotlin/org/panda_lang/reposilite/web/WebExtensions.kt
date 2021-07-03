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
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.HttpOutput
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.api.DocumentInfo
import org.panda_lang.utilities.commons.function.Result
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Extends Javalin's context with a support for [ErrorResponse] results
 */
fun Context.error(error: ErrorResponse): Context =
    status(error.status).json(error)

fun Context.contentLength(length: Long): Context =
    also { res.setContentLengthLong(length) }

fun Context.encoding(encoding: String): Context =
    also { res.characterEncoding = encoding }

fun Context.contentDisposition(disposition: String): Context =
    header("Content-Disposition", disposition)

fun Context.resultAttachment(document: DocumentInfo): Context {
    val data = document.content()

    if (method() != "HEAD") {
        data.transferLargeTo(res.outputStream)
    }
    else {
        data.close()
    }

    if (document.isReadable()) {
        contentDisposition(""""attachment; filename="${document.name}" """)
    }

    contentType(document.contentType)
    contentLength(document.contentLength)

    return this
}

fun InputStream.transferLargeTo(outputStream: OutputStream): Boolean =
    if (outputStream.isProbablyOpen()) {
        IOUtils.copyLarge(this, outputStream)
        true
    }
    else false

fun OutputStream.isProbablyOpen(): Boolean =
    when (this) {
        is HttpOutput -> !isClosed
        else -> true
    }

fun String.toPath(): Path =
    Paths.get(this)

/**
 * Project non-existing value of errored [Result] to simplify error handling by convenient way to match expected signatures.
 * This method throws [IllegalArgumentException] if the given [Result] does not contain error.
 */
fun <ANY_VALUE, REQUIRED_VALUE, ERROR> Result<ANY_VALUE, ERROR>.projectToError(): Result<REQUIRED_VALUE, ERROR> =
    if (this.isErr) this.map { null } else throw IllegalArgumentException("")

fun <VALUE, ERROR> Result<VALUE, ERROR>.filter(predicate: (VALUE) -> Boolean, error: () -> ERROR): Result<VALUE, ERROR> =
    if (this.isOk && predicate(get())) this else Result.error(error())

fun <VALUE> Result<VALUE, *>.orNull(): VALUE? =
    this.orElseGet { null }

fun <VALUE, ERROR> VALUE.asResult(): Result<VALUE, ERROR> =
    Result.ok(this)