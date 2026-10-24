/*
 * Copyright (c) 2020-2026 dzikoysk
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

import com.reposilite.auth.api.Credentials
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.AccessTokenFacade
import com.reposilite.token.api.AccessTokenDto
import panda.std.Result
import panda.std.asSuccess

/** Accepts web interface sessions created by [OidcService.login] */
internal class OidcAuthenticator(
    private val oidcService: OidcService,
    private val accessTokenFacade: AccessTokenFacade,
) : Authenticator {

    override fun authenticate(credentials: Credentials): Result<AccessTokenDto, ErrorResponse> =
        credentials.takeIf { oidcService.isValidSession(it.name, it.secret) }
            ?.let { accessTokenFacade.getAccessToken(it.name) }
            ?.asSuccess()
            ?: unauthorizedError("Invalid OIDC session")

    override fun enabled(): Boolean =
        oidcService.isEnabled()

    override fun priority(): Double =
        1.0

    override fun realm(): String =
        "OIDC"

}
