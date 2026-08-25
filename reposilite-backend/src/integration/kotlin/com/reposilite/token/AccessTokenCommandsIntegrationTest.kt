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

@file:Suppress("FunctionName")

package com.reposilite.token

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.console.CommandStatus.FAILED
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.console.ConsoleFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.specification.AccessTokenIntegrationSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import panda.std.ResultAssertions.assertOk

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalAccessTokenCommandsIntegrationTest : AccessTokenCommandsIntegrationTest()

internal abstract class AccessTokenCommandsIntegrationTest : AccessTokenIntegrationSpecification() {

    private val consoleFacade by lazy { reposilite.extensions.facade<ConsoleFacade>() }

    @Test
    fun `should modify access token permissions`() {
        // given: a token
        val (name) = useAuth("name", "secret", listOf(), routes = mapOf("/" to READ))

        // when: user updates the token
        val preparedCommand = assertOk(consoleFacade.prepareCommand("token-modify name m"))
        val firstResult = preparedCommand.execute()
        /**
         * Make sure that modification of token is properly handled by respecting the UNIQUE constraint
         * ~ https://github.com/dzikoysk/reposilite/issues/1321
         */
        val secondResult = assertOk(consoleFacade.executeCommand("token-modify name m"))

        // then: the given token is updated
        assertThat(firstResult.status).isEqualTo(SUCCEEDED)
        assertThat(secondResult.status).isEqualTo(SUCCEEDED)
        assertThat(preparedCommand.redactedCommand).isEqualTo("token-modify name m")
        assertThat(useExistingToken(name).permissions).isEqualTo(setOf(MANAGER))
    }

    @Test
    fun `should redact explicitly provided access token secret`() {
        // given: a command with an explicitly provided secret
        val secret = "audit-secret"

        // when: user generates a token
        val preparedCommand = assertOk(consoleFacade.prepareCommand("token-generate --secret=$secret --silent audited"))
        val result = preparedCommand.execute()

        // then: the command is audited without the secret
        assertThat(result.status).isEqualTo(SUCCEEDED)
        assertThat(preparedCommand.redactedCommand).isEqualTo("token-generate <redacted>")
        assertThat(useFacade<AccessTokenFacade>().secretMatches(useExistingToken("audited").accessToken.identifier, secret)).isTrue
    }

    @Test
    fun `should preserve access token arguments when secret is generated`() {
        // given: a command without an explicitly provided secret
        val rawCommand = "token-generate --silent generated"

        // when: user generates a token
        val preparedCommand = assertOk(consoleFacade.prepareCommand(rawCommand))
        val result = preparedCommand.execute()

        // then: the command is audited with its non-sensitive arguments
        assertThat(result.status).isEqualTo(SUCCEEDED)
        assertThat(preparedCommand.redactedCommand).isEqualTo(rawCommand)
    }

    @Test
    fun `should regenerate access token secret`() {
        // given: a token
        val (name) = useAuth("name", "secret", listOf(), routes = mapOf("/" to READ))

        // when: user updates the token
        val preparedCommand = assertOk(consoleFacade.prepareCommand("token-regenerate name -s new-secret"))
        val firstResult = preparedCommand.execute()

        // then: the given token is updated
        assertThat(firstResult.status).isEqualTo(SUCCEEDED)
        assertThat(preparedCommand.redactedCommand).isEqualTo("token-regenerate <redacted>")
        assertThat(useFacade<AccessTokenFacade>().secretMatches(useExistingToken(name).accessToken.identifier, "new-secret")).isTrue
    }

    @Test
    fun `should redact command arguments when parsing fails`() {
        // given: an invalid command containing a secret
        val secret = "parse-failure-secret"

        // when: user executes the command
        val preparedCommand = assertOk(
            consoleFacade.prepareCommand("token-generate --secret=$secret --silent audited unexpected")
        )
        val result = preparedCommand.execute()

        // then: the command fails without preserving its arguments
        assertThat(result.status).isEqualTo(FAILED)
        assertThat(preparedCommand.redactedCommand).isEqualTo("token-generate <redacted>")
        assertThat(result.response.joinToString()).doesNotContain(secret)
    }

}
