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
import com.reposilite.token.specification.AccessTokenSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class BasicAuthenticatorTest : AccessTokenSpecification() {

    @Test
    fun `should reject stored whitespace-only token secrets`() {
        // given: a token has a whitespace-only secret
        val secret = " "
        val token = createToken("Alice Cullen", secret)
        assertThat(accessTokenFacade.secretMatches(token.identifier, secret)).isTrue()

        // when: authentication is requested with that secret
        val authenticationResult = BasicAuthenticator(accessTokenFacade).authenticate(
            Credentials(
                ip = "host",
                name = "Alice Cullen",
                secret = secret
            )
        )

        // then: authentication is rejected despite the matching stored secret
        assertThat(assertError(authenticationResult).status).isEqualTo(401)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `should reject NUL characters matching a stored blank secret`(secret: String) {
        // given: BCrypt accepts a NUL-containing password for a stored blank secret
        val token = createToken("Alice Cullen", secret)
        val password = "$secret\u0000$secret"
        assertThat(accessTokenFacade.secretMatches(token.identifier, password)).isTrue()

        // when: authentication is requested with the equivalent password
        val authenticationResult = BasicAuthenticator(accessTokenFacade).authenticate(
            Credentials(
                ip = "host",
                name = "Alice Cullen",
                secret = password
            )
        )

        // then: authentication is rejected despite BCrypt accepting the password
        assertThat(assertError(authenticationResult).status).isEqualTo(401)
    }

    @ParameterizedTest
    @ValueSource(strings = ["secret", " secret "])
    fun `should authenticate with the exact token secret`(secret: String) {
        // given: a token with a nonblank secret
        val token = createToken("Alice Cullen", secret)

        // when: authentication is requested with that secret
        val authenticationResult = BasicAuthenticator(accessTokenFacade).authenticate(
            Credentials(
                ip = "host",
                name = "Alice Cullen",
                secret = secret
            )
        )

        // then: the existing token is returned
        assertOk(token, authenticationResult)
    }

}
