/*
 * Copyright (c) 2022 dzikoysk
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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.reposilite.auth.api.Credentials
import com.reposilite.auth.api.SessionDetails
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.api.AccessTokenDto
import panda.std.Result
import panda.std.asSuccess
import java.util.concurrent.TimeUnit.MINUTES

class AuthenticationFacade(
    private val journalist: Journalist,
    private val authenticators: List<Authenticator>,
    private val accessTokenFacade: AccessTokenFacade
) : Journalist, Facade {

    private val authenticationCache: Cache<Credentials, AccessTokenDto> = CacheBuilder.newBuilder()
        .maximumSize(16)
        .expireAfterAccess(1, MINUTES)
        .build()

    fun authenticateByCredentials(credentials: Credentials): Result<out AccessTokenDto, ErrorResponse> =
        authenticationCache.getIfPresent(credentials)
            ?.asSuccess()
            ?: authenticators.asSequence()
                .filter { it.enabled() }
                .map { authenticator ->
                    authenticator
                        .authenticate(credentials)
                        .onError { logger.debug("${credentials.name} failed to authenticate with ${authenticator.realm()} realm due to $it") }
                }
                .firstOrNull { it.isOk }
                ?.peek { authenticationCache.put(credentials, it) }
            ?: unauthorizedError("Invalid authorization credentials")

    fun getSessionDetails(identifier: AccessTokenIdentifier): Result<SessionDetails, ErrorResponse> =
        accessTokenFacade.getAccessTokenById(identifier)
            ?.let {
                SessionDetails(
                    it,
                    accessTokenFacade.getPermissions(it.identifier),
                    accessTokenFacade.getRoutes(it.identifier)
                )
            }
            ?.asSuccess()
            ?: notFoundError("Token $identifier not found")

    override fun getLogger(): Logger =
        journalist.logger

}
