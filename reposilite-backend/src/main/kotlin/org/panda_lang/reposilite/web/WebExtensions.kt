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
import org.panda_lang.reposilite.maven.api.FileDetails
import org.panda_lang.utilities.commons.function.Result
import java.io.InputStream

/**
 * Extends Javalin's context with a support for [ErrorResponse] results
 */
fun Context.error(error: ErrorResponse): Context =
    this.status(error.status).json(error)

fun Context.contentLength(length: Long): Context =
    also { res.setContentLengthLong(length) }

fun Context.encoding(encoding: String): Context =
    also { res.characterEncoding = encoding }

fun Context.resultAttachment(fileDetailsResponse: FileDetails, data: InputStream): Context =
    this.also {
            if (method() != "HEAD") data.transferTo(res.outputStream)
            else data.close()
        }
        .contentType(fileDetailsResponse.contentType)
        .contentLength(fileDetailsResponse.contentLength)
        .header("Content-Disposition", if (fileDetailsResponse.isReadable()) "" else """"attachment; filename="${fileDetailsResponse.name}" """)

/**
 * Project non-existing value of errored [Result] to simplify error handling by convenient way to match expected signatures.
 * This method throws [IllegalArgumentException] if the given [Result] does not contain error.
 */
fun <ANY_VALUE, REQUIRED_VALUE, ERROR> Result<ANY_VALUE, ERROR>.projectToError(): Result<REQUIRED_VALUE, ERROR> =
    if (this.isErr) this.map { null } else throw IllegalArgumentException("")
