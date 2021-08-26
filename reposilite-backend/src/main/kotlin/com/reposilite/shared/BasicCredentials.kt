package com.reposilite.shared

import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asSuccess
import java.util.Base64

const val AUTHORIZATION_HEADER = "Authorization"

fun extractFromHeaders(headers: Map<String, String>): Result<Pair<String, String>, ErrorResponse> =
    extractFromHeader(headers[AUTHORIZATION_HEADER])

fun extractFromHeader(authorizationHeader: String?): Result<Pair<String, String>, ErrorResponse> =
    authorizationHeader
        ?.takeIf { it.startsWith("Basic") }
        ?.substring("Basic".length)
        ?.trim()
        ?.let { extractFromBase64(it) }
        ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")


fun extractFromBase64(basicCredentials: String): Result<Pair<String, String>, ErrorResponse> =
    extractFromString(Base64.getDecoder().decode(basicCredentials).decodeToString())

fun extractFromString(credentials: String): Result<Pair<String, String>, ErrorResponse> =
    credentials
        .split(":", limit = 2)
        .takeIf { it.size == 2 }
        ?.let { (username, password) -> Pair(username, password).asSuccess() }
        ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")