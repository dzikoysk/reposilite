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

import com.reposilite.failure.api.ErrorResponse
import com.reposilite.maven.api.DocumentInfo
import io.javalin.http.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.HttpOutput
import panda.std.Result
import panda.std.Result.ok
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Extends Javalin's context with a support for [ErrorResponse] results
 */
fun Context.error(error: ErrorResponse): Context =
    status(error.status).json(error)

fun Context.contentLength(length: Long): Context =
    also { res.setContentLengthLong(length) }

fun Context.encoding(encoding: Charset): Context =
    encoding(encoding.name())

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

fun <ANY> ANY.alsoIf(condition: Boolean, block: (ANY) -> Unit): ANY {
    if (condition) block(this)
    return this
}

fun <VALUE, ERROR> VALUE.asResult(): Result<VALUE, ERROR> =
    Result.ok(this)

fun <VALUE, ERROR, REQUIRED_ERROR> Result<VALUE, ERROR>.projectToValue(): Result<VALUE, REQUIRED_ERROR> {
    if (isErr) {
        throw IllegalStateException("Cannot project result with error to value")
    }

    return ok(this.get())
}

suspend fun <VALUE, ERROR, MAPPED_ERROR> Flow<Result<out VALUE, ERROR>>.firstSuccessOr(elseValue: suspend () -> Result<out VALUE, MAPPED_ERROR>): Result<out VALUE, MAPPED_ERROR> =
    this.firstOrNull { it.isOk }
        ?.projectToValue()
        ?: elseValue()

suspend fun <VALUE, ERROR> Flow<Result<out VALUE, ERROR>>.firstOrErrors(): Result<out VALUE, Collection<ERROR>> {
    val collection: MutableCollection<ERROR> = ArrayList()

    return this
        .map { result -> result.onError { collection.add(it) } }
        .firstSuccessOr { Result.error(collection) }
}