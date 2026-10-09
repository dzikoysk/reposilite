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


package com.reposilite.auth.oauth2

import com.reposilite.auth.Authenticator
import com.reposilite.auth.api.Credentials
import com.reposilite.shared.ErrorResponse
import com.reposilite.token.api.AccessTokenDto
import panda.std.Result

/**
 * Authenticates dashboard users that signed in through identity provider using credentials of their session.
 */
internal class OAuth2SessionAuthenticator(private val oauth2Service: OAuth2Service) : Authenticator {

    override fun authenticate(credentials: Credentials): Result<AccessTokenDto, ErrorResponse> =
        oauth2Service.authenticateSession(credentials.name, credentials.secret)

    override fun enabled(): Boolean =
        oauth2Service.isEnabled()

    // Session lookup is cheaper than verification of access token's secret
    override fun priority(): Double =
        1.0

    override fun realm(): String =
        "OAuth2 Session"

    // Sessions expire and can be closed, so the result cannot outlive them
    override fun cacheable(): Boolean =
        false

}

/**
 * Authenticates clients using JWT issued by identity provider, provided as bearer token or as a secret of basic credentials.
 */
internal class OAuth2BearerAuthenticator(private val oauth2Service: OAuth2Service) : Authenticator {

    override fun authenticate(credentials: Credentials): Result<AccessTokenDto, ErrorResponse> =
        oauth2Service.authenticateBearer(credentials.secret)

    override fun enabled(): Boolean =
        oauth2Service.isBearerEnabled()

    override fun priority(): Double =
        -0.5

    override fun realm(): String =
        "OAuth2 Bearer"

    // Tokens expire, so each request has to be verified
    override fun cacheable(): Boolean =
        false

}
