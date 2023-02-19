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

@file:Suppress("FunctionName")

package com.reposilite.token

import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.console.ConsoleFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.specification.AccessTokenIntegrationSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import panda.std.ResultAssertions.assertOk

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalAccessTokenCommandsIntegrationTest : AccessTokenCommandsIntegrationTest()

internal abstract class AccessTokenCommandsIntegrationTest : AccessTokenIntegrationSpecification() {

    private val consoleFacade by lazy { reposilite.extensions.facade<ConsoleFacade>() }

    @Test
    fun `should modify access token permissions`() {
        // given: a token
        val (name) = useAuth("name", "secret", listOf(), routes = mapOf("/" to READ))

        // when: user updates the token
        val firstResult = assertOk(consoleFacade.executeCommand("token-modify name m"))
        /**
         * Make sure that modification of token is properly handled by respecting the UNIQUE constraint
         * ~ https://github.com/dzikoysk/reposilite/issues/1321
         */
        val secondResult = assertOk(consoleFacade.executeCommand("token-modify name m"))

        // then: the given token is updated
        assertThat(firstResult.status).isEqualTo(SUCCEEDED)
        assertThat(secondResult.status).isEqualTo(SUCCEEDED)
        assertThat(useExistingToken(name).permissions).isEqualTo(setOf(MANAGER))
    }

    @Test
    fun `should regenerate access token secret`() {
        // given: a token
        val (name) = useAuth("name", "secret", listOf(), routes = mapOf("/" to READ))

        // when: user updates the token
        val firstResult = assertOk(consoleFacade.executeCommand("token-regenerate name -s new-secret"))

        // then: the given token is updated
        assertThat(firstResult.status).isEqualTo(SUCCEEDED)
        assertThat(useFacade<AccessTokenFacade>().secretMatches(useExistingToken(name).accessToken.identifier, "new-secret")).isTrue
    }

}