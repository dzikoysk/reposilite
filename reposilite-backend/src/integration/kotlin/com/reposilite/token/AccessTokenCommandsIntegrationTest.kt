package com.reposilite.token

import com.reposilite.LocalSpecificationJunitExtension
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.console.ConsoleFacade
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.specification.AccessTokenIntegrationSpecification
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(SUCCEEDED, firstResult.status)
        assertEquals(SUCCEEDED, secondResult.status)
        assertEquals(setOf(MANAGER), useExistingToken(name).permissions)
    }

}