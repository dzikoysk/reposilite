package com.reposilite.token

import com.reposilite.ReposiliteJournalist
import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.specification.AccessTokenSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

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
        val journalist = ReposiliteJournalist(InMemoryLogger(), 0, testEnv = true, noColor = false)
        val context = CommandContext(journalist)

        // when: tokens are exported to file
        val exportCommand = ExportTokensCommand(workingDirectory, accessTokenFacade).also { it.name = fileName }
        exportCommand.execute(context)

        // then: tokens were successfully exported
        assertThat(context.status).isEqualTo(SUCCEEDED)
        assertThat(workingDirectory.resolve(fileName).exists()).isTrue

        // given: a facade without tokens
        deleteAllTokens()

        // when: exported tokens are imported from the given file
        val importCommand = ImportTokensCommand(workingDirectory, accessTokenFacade).also { it.name = fileName }
        importCommand.execute(context)

        // then: the imported tokens match the old ones
        assertThat(context.status).isEqualTo(SUCCEEDED)

        tokens.forEach {
            val token = accessTokenFacade.getAccessTokenDetailsById(accessTokenFacade.getAccessToken(it.accessToken.name)!!.identifier)!!
            assertThat(token.accessToken.copy(identifier = it.accessToken.identifier)).isEqualTo(it.accessToken)
            assertThat(token.permissions).isEqualTo(it.permissions)
            assertThat(token.routes).isEqualTo(it.routes)
        }

        for (message in context.output()) {
            println(message)
        }
    }

}
