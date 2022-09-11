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

import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.UNAUTHORIZED
import panda.std.Result
import panda.std.asError

data class ErrorResponse(val status: Int, val message: String) {

    constructor(code: HttpStatus, message: String) : this(code.code, message)

    fun updateMessage(transform: (String) -> String): ErrorResponse =
        ErrorResponse(status, transform(message))

}

fun <V> errorResponse(status: HttpStatus, message: String): Result<V, ErrorResponse> =
    Result.error(ErrorResponse(status.code, message))

fun HttpStatus.toErrorResponse(message: String? = null): ErrorResponse =
    ErrorResponse(this, message ?: this.message)

fun <V> HttpStatus.toErrorResult(message: String? = null): Result<V, ErrorResponse> =
    toErrorResponse(message).asError()

fun <V> notFoundError(message: String? = null): Result<V, ErrorResponse> =
    notFound(message).asError()

fun notFound(message: String? = null): ErrorResponse =
    NOT_FOUND.toErrorResponse(message)

fun <V> unauthorizedError(message: String? = null): Result<V, ErrorResponse> =
    unauthorized(message).asError()

fun unauthorized(message: String? = null): ErrorResponse =
    UNAUTHORIZED.toErrorResponse(message)

fun <V> badRequestError(message: String? = null): Result<V, ErrorResponse> =
    badRequest(message).asError()

fun badRequest(message: String? = null): ErrorResponse =
    BAD_REQUEST.toErrorResponse(message)

fun internalServer(message: String? = null): ErrorResponse =
    INTERNAL_SERVER_ERROR.toErrorResponse(message)

fun <V> internalServerError(message: String? = null): Result<V, ErrorResponse> =
    internalServer(message).asError()
