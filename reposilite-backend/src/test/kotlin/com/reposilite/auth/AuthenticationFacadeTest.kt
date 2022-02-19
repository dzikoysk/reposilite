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

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.auth.specification.AuthenticationSpecification
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class AuthenticationFacadeTest : AuthenticationSpecification() {

    @Test
    fun `should reject authentication request with invalid credentials`() = runBlocking {
        // given: an existing token with unknown secret
        val name = "name"
        createToken(name)

        // when: an authentication is requested with invalid credentials
        val response = authenticationFacade.authenticateByCredentials(AuthenticationRequest(name, "invalid-secret"))

        // then: the request has been rejected
        assertError(ErrorResponse(UNAUTHORIZED, "Invalid authorization credentials"), response)
    }

    @Test
    fun `should authenticate by valid credentials`() = runBlocking {
        // given: a credentials to the existing token
        val name = "name"
        val secret = "secret"
        val accessToken = createToken(name, secret)

        // when: an authentication is requested with valid credentials
        val response = authenticationFacade.authenticateByCredentials(AuthenticationRequest(name, secret))

        // then: the request has been authorized
        assertOk(accessToken, response)
    }

}