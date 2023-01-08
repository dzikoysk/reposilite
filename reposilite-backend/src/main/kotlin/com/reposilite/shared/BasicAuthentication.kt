/*
 * Copyright (c) 2023 dzikoysk
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

import io.javalin.security.BasicAuthCredentials
import panda.std.Result
import panda.std.asSuccess
import java.util.Base64

fun extractFromHeader(authorizationHeader: String?): Result<BasicAuthCredentials, ErrorResponse> {
    if (authorizationHeader == null) {
        return unauthorizedError("Missing authorization credentials")
    }

    val method = when {
        authorizationHeader.startsWith("Basic") -> "Basic" // Standard basic auth
        authorizationHeader.startsWith("xBasic") -> "xBasic" // Basic auth for browsers to avoid built-in auth popup
        else -> return unauthorizedError("Unknown authorization method")
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
        ?: unauthorizedError("Invalid authorization credentials format")
