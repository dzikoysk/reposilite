package com.reposilite.shared

import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asError

private const val NOT_FOUND_MESSAGE = "Not found"

fun <V> notFoundError(message: String = NOT_FOUND_MESSAGE): Result<V, ErrorResponse> =
    notFound(message).asError()

fun notFound(message: String = NOT_FOUND_MESSAGE): ErrorResponse =
    ErrorResponse(NOT_FOUND, message)

private const val UNAUTHORIZED_MESSAGE = "Unauthorized access request"

fun <V> unauthorizedError(message: String = UNAUTHORIZED_MESSAGE): Result<V, ErrorResponse> =
    unauthorized(message).asError()

fun unauthorized(message: String = UNAUTHORIZED_MESSAGE): ErrorResponse =
    ErrorResponse(UNAUTHORIZED, message)