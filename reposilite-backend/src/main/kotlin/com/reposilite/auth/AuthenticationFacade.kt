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

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.auth.api.SessionDetails
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import com.reposilite.web.http.unauthorizedError
import panda.std.Result
import panda.std.asSuccess

class AuthenticationFacade(
    private val journalist: Journalist,
    private val authenticators: List<Authenticator>,
    private val accessTokenFacade: AccessTokenFacade
) : Journalist, Facade {

    fun authenticateByCredentials(authenticationRequest: AuthenticationRequest): Result<out AccessTokenDto, ErrorResponse> =
        authenticators.asSequence()
            .filter { it.enabled() }
            .map { authenticator -> authenticator
                .authenticate(authenticationRequest)
                .onError { logger.debug("${authenticationRequest.name} failed to authenticate with ${authenticator.realm()} realm due to $it")  }
            }
            .firstOrNull { it.isOk }
            ?: unauthorizedError("Invalid authorization credentials")

    fun geSessionDetails(identifier: AccessTokenIdentifier): Result<SessionDetails, ErrorResponse> =
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