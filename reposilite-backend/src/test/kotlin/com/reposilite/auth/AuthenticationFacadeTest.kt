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

package com.reposilite.auth

import com.reposilite.auth.api.Credentials
import com.reposilite.auth.application.BruteForceProtectionSettings
import com.reposilite.auth.specification.AuthenticationSpecification
import com.reposilite.shared.unauthorized
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
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
        val response = authenticationFacade.authenticateByCredentials(Credentials("host", name, "invalid-secret"))

        // then: the request has been rejected
        assertError(unauthorized("Invalid authorization credentials"), response)
    }

    @Test
    fun `should authenticate by valid credentials`() = runBlocking {
        // given: a credentials to the existing token
        val name = "name"
        val secret = "secret"
        val accessToken = createToken(name, secret)

        // when: an authentication is requested with valid credentials
        val response = authenticationFacade.authenticateByCredentials(Credentials("host", name, secret))

        // then: the request has been authorized
        assertOk(accessToken, response)
    }

    @Test
    fun `should block IP after exceeding max failed attempts`() = runBlocking {
        // given: brute force protection is enabled with max 3 attempts
        bruteForceProtectionSettings.update(BruteForceProtectionSettings(enabled = true, maxAttempts = 3, banDurationSeconds = 300))
        val name = "name"
        val secret = "secret"
        createToken(name, secret)

        // when: 3 failed authentication attempts are made from the same IP
        repeat(3) {
            authenticationFacade.authenticateByCredentials(Credentials("attacker-ip", name, "wrong-secret"))
        }

        // then: even valid credentials are rejected from that IP
        val response = authenticationFacade.authenticateByCredentials(Credentials("attacker-ip", name, secret))
        assertTrue(response.isErr)
    }

    @Test
    fun `should allow different IPs when one is blocked`() = runBlocking {
        // given: brute force protection is enabled and one IP is blocked
        bruteForceProtectionSettings.update(BruteForceProtectionSettings(enabled = true, maxAttempts = 3, banDurationSeconds = 300))
        val name = "name"
        val secret = "secret"
        createToken(name, secret)

        repeat(3) {
            authenticationFacade.authenticateByCredentials(Credentials("blocked-ip", name, "wrong-secret"))
        }

        // when: a different IP authenticates with valid credentials
        val response = authenticationFacade.authenticateByCredentials(Credentials("other-ip", name, secret))

        // then: the request is authorized
        assertOk(response)
    }

    @Test
    fun `should not block when brute force protection is disabled`() = runBlocking {
        // given: brute force protection is disabled (default)
        bruteForceProtectionSettings.update(BruteForceProtectionSettings(enabled = false, maxAttempts = 3, banDurationSeconds = 300))
        val name = "name"
        val secret = "secret"
        createToken(name, secret)

        // when: many failed attempts are made followed by a valid one
        repeat(10) {
            authenticationFacade.authenticateByCredentials(Credentials("some-ip", name, "wrong-secret"))
        }
        val response = authenticationFacade.authenticateByCredentials(Credentials("some-ip", name, secret))

        // then: the valid request is still authorized
        assertOk(response)
    }

    @Test
    fun `should clear counter on successful authentication`() = runBlocking {
        // given: brute force protection is enabled and some failed attempts exist
        bruteForceProtectionSettings.update(BruteForceProtectionSettings(enabled = true, maxAttempts = 3, banDurationSeconds = 300))
        val name = "name"
        val secret = "secret"
        createToken(name, secret)

        // when: 2 failed attempts, then a success, then 2 more failed attempts
        repeat(2) {
            authenticationFacade.authenticateByCredentials(Credentials("user-ip", name, "wrong-secret"))
        }
        authenticationFacade.authenticateByCredentials(Credentials("user-ip", name, secret))
        repeat(2) {
            authenticationFacade.authenticateByCredentials(Credentials("user-ip", name, "wrong-secret"))
        }

        // then: the IP is not blocked (counter was reset on success)
        val response = authenticationFacade.authenticateByCredentials(Credentials("user-ip", name, secret))
        assertOk(response)
    }

}
