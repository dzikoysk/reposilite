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

import com.fasterxml.jackson.databind.ObjectMapper
import com.reposilite.auth.application.OidcSettings
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenType.PERSISTENT
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.specification.AccessTokenSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.reactive.toMutableReference
import java.time.Instant

internal class OidcServiceTest : AccessTokenSpecification() {

    private val oidcService = OidcService(logger, OidcSettings(enabled = true).toMutableReference(), accessTokenFacade)

    private fun userinfo(json: String) = ObjectMapper().readTree(json)

    @Test
    fun `should create user, sync manager role and issue session`() {
        val session = oidcService.login(userinfo("""{ "email": "john@company.com", "roles": ["reposilite-manager"] }""")).get()
        val user = accessTokenFacade.getAccessToken("john@company.com")!!

        assertThat(session.name).isEqualTo("john@company.com")
        assertThat(accessTokenFacade.hasPermission(user.identifier, MANAGER)).isTrue
        assertThat(oidcService.isValidSession("john@company.com", session.secret)).isTrue
        assertThat(oidcService.isValidSession("eve@company.com", session.secret)).isFalse
        assertThat(oidcService.isValidSession("john@company.com", session.secret + "x")).isFalse
        assertThat(oidcService.isValidSession("john@company.com", "1.${session.secret.substringAfter('.')}")).isFalse

        oidcService.login(userinfo("""{ "email": "john@company.com", "roles": ["reposilite-user"] }"""))
        assertThat(accessTokenFacade.hasPermission(user.identifier, MANAGER)).isFalse

        // roles only in token claims (e.g. FusionAuth with strict scope handling)
        oidcService.login(userinfo("""{ "email": "john@company.com" }"""), listOf(userinfo("""{ "roles": ["reposilite-manager"] }""")))
        assertThat(accessTokenFacade.hasPermission(user.identifier, MANAGER)).isTrue
    }

    @Test
    fun `should reject unusable user names`() {
        assertThat(oidcService.login(userinfo("""{ "sub": "123" }""")).error.message).contains("available claims: sub")
        assertThat(oidcService.login(userinfo("""{ "email": "john#laptop" }""")).isErr).isTrue
    }

    @Test
    fun `should refuse unverified e-mail addresses and normalize case`() {
        assertThat(oidcService.login(userinfo("""{ "email": "john@company.com", "email_verified": false, "roles": ["reposilite-user"] }""")).error.status).isEqualTo(403)
        assertThat(accessTokenFacade.getAccessToken("john@company.com")).isNull()

        assertThat(oidcService.login(userinfo("""{ "email": "John@Company.com", "email_verified": true, "roles": ["reposilite-user"] }""")).get().name).isEqualTo("john@company.com")
        assertThat(accessTokenFacade.getAccessToken("john@company.com")).isNotNull
    }

    @Test
    fun `should never take over tokens that are not oidc users`() {
        val admin = accessTokenFacade.createAccessToken(CreateAccessTokenRequest(type = PERSISTENT, name = "admin@company.com")).accessToken
        accessTokenFacade.addPermission(admin.identifier, MANAGER)

        val refused = oidcService.login(userinfo("""{ "email": "admin@company.com", "roles": ["reposilite-user"] }"""))
        assertThat(refused.error.status).isEqualTo(403)
        assertThat(accessTokenFacade.hasPermission(admin.identifier, MANAGER)).isTrue
    }

    @Test
    fun `should limit and clean up user tokens`() {
        oidcService.login(userinfo("""{ "email": "john@company.com", "roles": ["reposilite-user"] }"""))
        val user = accessTokenFacade.getAccessToken("john@company.com")!!
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(type = PERSISTENT, name = "john@company.com#old", expiresAt = Instant.now().minusSeconds(1)))

        assertThat(oidcService.getUserTokens(user).get()).isEmpty()
        assertThat(accessTokenFacade.getAccessToken("john@company.com#old")).isNull()

        repeat(20) { assertThat(oidcService.createUserToken(user, "t$it", emptySet()).isOk).isTrue }
        assertThat(oidcService.createUserToken(user, "t20", emptySet()).error.status).isEqualTo(400)
    }

    @Test
    fun `should only let users with user or manager role sign in`() {
        val refused = oidcService.login(userinfo("""{ "email": "marketing@company.com", "roles": ["marketing"] }"""))
        assertThat(refused.error.status).isEqualTo(403)
        assertThat(accessTokenFacade.getAccessToken("marketing@company.com")).isNull()

        val everyone = OidcService(logger, OidcSettings(enabled = true, userRole = "").toMutableReference(), accessTokenFacade)
        assertThat(everyone.login(userinfo("""{ "email": "marketing@company.com" }""")).isOk).isTrue
    }

    @Test
    fun `user tokens should never exceed current user routes`() {
        oidcService.login(userinfo("""{ "email": "john@company.com", "roles": ["reposilite-user"] }"""))
        val user = accessTokenFacade.getAccessToken("john@company.com")!!
        accessTokenFacade.addRoute(user.identifier, Route("/releases", READ))

        val tooMuch = oidcService.createUserToken(user, "laptop", setOf(CreateAccessTokenRequest.Route("/releases", setOf(READ, WRITE))))
        assertThat(tooMuch.error.status).isEqualTo(403)

        val created = oidcService.createUserToken(user, "laptop", setOf(CreateAccessTokenRequest.Route("/releases/com/company", setOf(READ)))).get()
        val token = created.accessToken
        assertThat(token.name).isEqualTo("john@company.com#laptop")
        assertThat(token.expiresAt).isNotNull
        assertThat(accessTokenFacade.hasPermissionTo(token.identifier, "/releases/com/company/app", READ)).isTrue
        assertThat(accessTokenFacade.hasPermissionTo(token.identifier, "/releases/org/other", READ)).isFalse
        assertThat(oidcService.getUserTokens(user).get()).extracting("name").containsExactly("john@company.com#laptop")
        assertThat(oidcService.createUserToken(token, "nested", emptySet()).isErr).isTrue

        accessTokenFacade.deleteRoute(user.identifier, Route("/releases", READ))
        assertThat(accessTokenFacade.hasPermissionTo(token.identifier, "/releases/com/company/app", READ)).isFalse
        assertThat(accessTokenFacade.canSee(token.identifier, "/releases")).isFalse

        oidcService.deleteUserToken(user, "laptop")
        assertThat(accessTokenFacade.getAccessToken("john@company.com#laptop")).isNull()
    }

}
