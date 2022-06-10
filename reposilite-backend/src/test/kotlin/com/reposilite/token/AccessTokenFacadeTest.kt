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

package com.reposilite.token

import com.reposilite.token.specification.AccessTokenSpecification
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.time.LocalDate

internal class AccessTokenFacadeTest : AccessTokenSpecification() {

    @Test
    fun `should create token`() = runBlocking {
        // given: a name and associated secret to register
        val name = "reposilite"

        // when: token is created with the given name
        val accessTokenDetails = createToken(name)

        // then: valid token should be created
        val accessToken = accessTokenDetails.accessToken
        assertEquals(name, accessToken.name)
        assertTrue(LocalDate.now().isEqual(accessToken.createdAt))
    }

    @Test
    fun `should update token` () = runBlocking {
        // given: an existing token and its updated version
        val token = createToken("nanomaven").accessToken
        val updatedToken = token.copy(name = "reposilite")

        // when: token is updated
        accessTokenFacade.updateToken(updatedToken)

        // then: stored token should be updated
        val storedToken = accessTokenFacade.getAccessToken("reposilite")!!
        assertEquals("reposilite", storedToken.name)
    }

    @Test
    fun `should delete token`() = runBlocking {
        // given: an existing token
        val token = createToken("reposilite").accessToken

        // when: token is deleted
        val result = accessTokenFacade.deleteToken(token.identifier)

        // then: proper token has been deleted, and it is no longer available
        assertOk(result)
        assertNull(accessTokenFacade.getAccessToken(token.name))
    }

    @Test
    fun `should find token by given name`() = runBlocking {
        // given: an existing token
        val token = createToken("reposilite").accessToken

        // when: token is requested by its name
        val foundToken = accessTokenFacade.getAccessToken(token.name)

        // then: proper token has been found
        assertEquals(token, foundToken)
    }

}