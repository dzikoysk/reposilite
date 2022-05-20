package com.reposilite.token

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.specification.AccessTokenSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class ExportAndImportTest : AccessTokenSpecification() {

    @Test
    fun `should export and import tokens`() {
        // given: a facade with 3 tokens
        val tokens = List(3) { createToken("token-$it", "secret-$it") }
            .forEach {
                accessTokenFacade.addPermission(it.identifier, MANAGER)
                accessTokenFacade.addRoute(it.identifier, Route("/test", READ))
            }
            .let { accessTokenFacade.getAccessTokens() }
            .map { accessTokenFacade.getAccessTokenDetailsById(it.identifier)!! }

        val fileName = "test.json"
        val context = CommandContext()

        // when: tokens are exported to file
        val exportCommand = ExportTokensCommand(workingDirectory(), accessTokenFacade).also { it.name = fileName }
        exportCommand.execute(context)

        // then: tokens were successfully exported
        assertEquals(SUCCEEDED, context.status)
        assertTrue(Files.exists(workingDirectory.toPath().resolve(fileName)))

        // given: a facade without tokens
        deleteAllTokens()

        // when: exported tokens are imported from the given file
        val importCommand = ImportTokensCommand(workingDirectory(), accessTokenFacade).also { it.name = fileName }
        importCommand.execute(context)

        // then: the imported tokens match the old ones
        assertEquals(SUCCEEDED, context.status)

        tokens.forEach {
            val token = accessTokenFacade.getAccessTokenDetailsById(accessTokenFacade.getAccessToken(it.accessToken.name)!!.identifier)!!
            assertEquals(it.accessToken, token.accessToken.copy(identifier = it.accessToken.identifier))
            assertEquals(it.permissions, token.permissions)
            assertEquals(it.routes, token.routes)
        }

        for (message in context.output()) {
            println(message)
        }
    }

}
