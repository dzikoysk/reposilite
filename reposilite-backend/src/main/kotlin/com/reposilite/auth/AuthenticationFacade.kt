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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.extractFromHeaders
import com.reposilite.web.http.extractFromString
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asSuccess

class AuthenticationFacade internal constructor(
    private val journalist: Journalist,
    private val accessTokenFacade: AccessTokenFacade
) : Journalist, Facade {

    fun authenticateByHeader(headers: Map<String, String>): Result<AccessTokenDto, ErrorResponse> =
        extractFromHeaders(headers)
            .flatMap { (name, secret) -> authenticateByCredentials(name, secret) }

    fun authenticateByCredentials(credentials: String): Result<AccessTokenDto, ErrorResponse> =
        extractFromString(credentials)
            .flatMap { (name, secret) -> authenticateByCredentials(name, secret) }

    fun authenticateByCredentials(name: String, secret: String): Result<AccessTokenDto, ErrorResponse> =
        accessTokenFacade.getAccessToken(name)
            ?.takeIf { accessTokenFacade.secretMatches(it.id, secret) }
            ?.asSuccess()
            ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")

    override fun getLogger(): Logger =
        journalist.logger

}