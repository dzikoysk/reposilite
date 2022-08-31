package com.reposilite.shared

import io.javalin.security.BasicAuthCredentials
import panda.std.Result
import panda.std.asSuccess
import java.util.Base64

const val AUTHORIZATION_HEADER = "Authorization"

fun extractFromHeaders(headers: Map<String, String>): Result<BasicAuthCredentials, ErrorResponse> =
    extractFromHeader(headers[AUTHORIZATION_HEADER])

fun extractFromHeader(authorizationHeader: String?): Result<BasicAuthCredentials, ErrorResponse> {
    if (authorizationHeader == null) {
        return unauthorizedError("Invalid authorization credentials")
    }

    val method = when {
        authorizationHeader.startsWith("Basic") -> "Basic" // Standard basic auth
        authorizationHeader.startsWith("xBasic") -> "xBasic" // Basic auth for browsers to avoid built-in auth popup
        else -> return unauthorizedError("Invalid authorization credentials")
    }

    return extractFromBase64(authorizationHeader.substring(method.length).trim())
}

fun extractFromBase64(basicCredentials: String): Result<BasicAuthCredentials, ErrorResponse> =
    extractFromString(Base64.getDecoder().decode(basicCredentials).decodeToString())

fun extractFromString(credentials: String): Result<BasicAuthCredentials, ErrorResponse> =
    credentials
        .split(":", limit = 2)
        .takeIf { it.size == 2 }
        ?.let { (username, password) -> BasicAuthCredentials(username, password).asSuccess() }
        ?: unauthorizedError("Invalid authorization credentials")
