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

package com.reposilite.auth

import com.reposilite.shared.extractFromBase64
import com.reposilite.shared.extractFromHeaders
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenFacade.Companion.B_CRYPT_TOKENS_ENCODER
import com.reposilite.token.api.AccessToken
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import panda.std.Result
import panda.std.asSuccess

class AuthenticationFacade internal constructor(
    private val journalist: Journalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val sessionService: SessionService
) : Journalist {

    fun authenticateByHeader(headers: Map<String, String>): Result<AccessToken, ErrorResponse> =
        extractFromHeaders(headers)
            .flatMap { (name, secret) -> authenticateByCredentials(name, secret) }

    fun authenticateByCredentials(credentials: String): Result<AccessToken, ErrorResponse> =
        extractFromBase64(credentials)
            .flatMap { (name, secret) -> authenticateByCredentials(name, secret) }

    fun authenticateByCredentials(name: String, secret: String): Result<AccessToken, ErrorResponse> =
        accessTokenFacade.getToken(name)
            ?.takeIf { B_CRYPT_TOKENS_ENCODER.matches(secret, it.secret) }
            ?.asSuccess()
            ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")

    fun createSession(path: String, method: SessionMethod, address: String, accessToken: AccessToken): Session =
        sessionService.createSession(path, method, address, accessToken)

    override fun getLogger(): Logger =
        journalist.logger

}