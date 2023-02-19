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

import com.reposilite.token.specification.AccessTokenSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
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
    fun `should regenerate token`() {
        // given: an existing token and its updated version
        val token = createToken("reposilite").accessToken

        // when: token is updated
        accessTokenFacade.regenerateAccessToken(token, "new secret")

        // then: stored token should be updated
        assertThat(accessTokenFacade.secretMatches(token.identifier, "new secret")).isTrue
    }
}
