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

package com.reposilite.token

import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.specification.AccessTokenSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.time.Instant
import java.time.LocalDate

internal class AccessTokenFacadeTest : AccessTokenSpecification() {

    @Test
    fun `should create token`() {
        // given: a name and associated secret to register
        val name = "reposilite"

        // when: token is created with the given name
        val accessTokenDetails = createToken(name)

        // then: valid token should be created
        val accessToken = accessTokenDetails.accessToken
        assertThat(accessToken.name).isEqualTo(name)
        assertThat(LocalDate.now().isEqual(accessToken.createdAt)).isTrue
    }

    @Test
    fun `should update token` () {
        // given: an existing token and its updated version
        val token = createToken("nanomaven").accessToken
        val updatedToken = token.copy(name = "reposilite")

        // when: token is updated
        accessTokenFacade.updateToken(updatedToken)

        // then: stored token should be updated
        val storedToken = accessTokenFacade.getAccessToken("reposilite")!!
        assertThat(storedToken.name).isEqualTo("reposilite")
    }

    @Test
    fun `should delete token`() {
        // given: an existing token
        val token = createToken("reposilite").accessToken

        // when: token is deleted
        val result = accessTokenFacade.deleteToken(token.identifier)

        // then: proper token has been deleted, and it is no longer available
        assertOk(result)
        assertThat(accessTokenFacade.getAccessToken(token.name)).isNull()
    }

    @Test
    fun `should find token by given name`() {
        // given: an existing token
        val token = createToken("reposilite").accessToken

        // when: token is requested by its name
        val foundToken = accessTokenFacade.getAccessToken(token.name)

        // then: proper token has been found
        assertThat(foundToken).isEqualTo(token)
    }

    @Test
    fun `should not find expired token by name`() {
        // given: a token that expires in the past
        val response = accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "expired-token",
                expiresAt = Instant.now().minusSeconds(60),
            )
        )

        // when: token is looked up by name
        val found = accessTokenFacade.getAccessToken("expired-token")

        // then: expired token should not be found
        assertThat(found).isNull()
    }

    @Test
    fun `should not find expired token by id`() {
        // given: a token that expires in the past
        val response = accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "expired-token",
                expiresAt = Instant.now().minusSeconds(60),
            )
        )

        // when: token is looked up by id
        val found = accessTokenFacade.getAccessTokenById(response.accessToken.identifier)

        // then: expired token should not be found
        assertThat(found).isNull()
    }

    @Test
    fun `should find non-expired token`() {
        // given: a token that expires in the future
        val response = accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "valid-token",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        )

        // when: token is looked up by name
        val found = accessTokenFacade.getAccessToken("valid-token")

        // then: non-expired token should be found
        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("valid-token")
    }

    @Test
    fun `should find token without expiration`() {
        // given: a token with no expiration (null expiresAt)
        val response = createToken("no-expiry-token")

        // when: token is looked up by name
        val found = accessTokenFacade.getAccessToken("no-expiry-token")

        // then: token with no expiration should always be found
        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("no-expiry-token")
    }

    @Test
    fun `should exclude expired tokens from findAll`() {
        // given: one expired and one valid token
        deleteAllTokens()
        accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "expired",
                expiresAt = Instant.now().minusSeconds(60),
            )
        )
        accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "valid",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        )

        // when: all tokens are listed
        val allTokens = accessTokenFacade.getAccessTokens()

        // then: only the valid token should be present
        assertThat(allTokens).hasSize(1)
        assertThat(allTokens.first().name).isEqualTo("valid")
    }

    @Test
    fun `should clean up routes and permissions of expired tokens`() {
        // given: an expired token with a route and permission
        deleteAllTokens()
        val response = accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = AccessTokenType.TEMPORARY,
                name = "expired-with-routes",
                expiresAt = Instant.now().minusSeconds(60),
                routes = setOf(CreateAccessTokenRequest.Route("/releases", setOf(RoutePermission.READ))),
                permissions = setOf(AccessTokenPermission.MANAGER),
            )
        )
        val tokenId = response.accessToken.identifier

        // when: the expired token is looked up (triggering eviction)
        val found = accessTokenFacade.getAccessToken("expired-with-routes")

        // then: token, routes, and permissions should all be cleaned up
        assertThat(found).isNull()
        assertThat(accessTokenFacade.getRoutes(tokenId)).isEmpty()
        assertThat(accessTokenFacade.getPermissions(tokenId)).isEmpty()
    }

    @Test
    fun `should regenerate token`() {
        // given: an existing token and its updated version
        val token = createToken("reposilite").accessToken

        // when: token is updated
        accessTokenFacade.regenerateAccessToken(token, "new secret")

        // then: stored token should be updated
        assertThat(accessTokenFacade.secretMatches(token.identifier, "new secret")).isTrue
    }
}
