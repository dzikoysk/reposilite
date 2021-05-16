/*
 * Copyright (c) 2020 Dzikoysk
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
package org.panda_lang.reposilite.auth

import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.AccessTokenFacade
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.function.Result
import java.nio.charset.StandardCharsets
import java.util.*

class Authenticator(private val accessTokenFacade: AccessTokenFacade) {

    fun authByUri(header: Map<String, String>, uri: String): Result<AccessToken, String> {
        var processedUri = uri

        if (!processedUri.startsWith("/")) {
            processedUri = "/$processedUri"
        }

        val authResult = authByHeader(header)

        if (authResult.isErr) {
            accessTokenFacade.logger.debug(authResult.error)
            return authResult
        }

        val token = authResult.get()

        if (!token.hasPermissionTo(processedUri)) {
            return Result.error("Unauthorized access attempt")
        }

        accessTokenFacade.logger.info("AUTH $token accessed $processedUri")
        return authResult
    }

    fun authByHeader(header: Map<String, String>): Result<AccessToken, String> {
        val authorization = header["Authorization"]

        accessTokenFacade.logger.debug("Header ---")
        header.forEach { (key, value) -> accessTokenFacade.logger.debug("$key: $value") }

        if (authorization == null) {
            return Result.error("Authorization credentials are not specified")
        }

        if (!authorization.startsWith("Basic")) {
            return Result.error("Unsupported auth method")
        }

        val base64Credentials = authorization.substring("Basic".length).trim()
        val credentials = String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8)

        return authByCredentials(credentials)
    }

    fun authByCredentials(credentials: String?): Result<AccessToken, String> {
        if (credentials == null) {
            return Result.error("Authorization credentials are not specified")
        }

        val values = StringUtils.splitFirst(credentials, ":")

        if (values.size != 2) {
            return Result.error("Invalid authorization credentials")
        }

        val tokenValue = accessTokenFacade.getToken(values[0])

        if (tokenValue.isEmpty) {
            return Result.error("Invalid authorization credentials")
        }

        val token = tokenValue.get()
        val authorized = AccessTokenFacade.B_CRYPT_TOKENS_ENCODER.matches(values[1], token.secret)

        if (!authorized) {
            return Result.error("Invalid authorization credentials")
        }

        return Result.ok(token)
    }

}