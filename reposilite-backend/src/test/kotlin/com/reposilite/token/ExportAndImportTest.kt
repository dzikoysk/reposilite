package com.reposilite.token

import com.reposilite.console.CommandContext
import com.reposilite.console.CommandStatus.SUCCEEDED
import com.reposilite.token.specification.AccessTokenSpecification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ExportAndImportTest : AccessTokenSpecification() {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `should export and import tokens`() {
        // given:
        val fileName = "test.json"
        val context = CommandContext()
        val tokens = repeat(3) { createToken("token-$it", "secret-$it") }
            .let { accessTokenFacade.getAccessTokens() }
            .map { accessTokenFacade.getAccessTokenDetailsById(it.identifier)!! }

        // when:
        val exportCommand = ExportTokensCommand(workingDirectory.toPath(), accessTokenFacade).also {
            it.name = fileName
        }
        exportCommand.execute(context)

        // then:
        assertEquals(SUCCEEDED, context.status)

        // when:
        deleteAllTokens()
        val importCommand = ImportTokensCommand(workingDirectory.toPath(), accessTokenFacade).also {
            it.name = fileName
        }
        importCommand.execute(context)

        // then:
        assertEquals(SUCCEEDED, context.status)

        tokens.forEach {
            val token = accessTokenFacade.getAccessTokenDetailsById(accessTokenFacade.getAccessToken(it.accessToken.name)!!.identifier)!!
            assertEquals(it.accessToken, token.accessToken.copy(identifier = it.accessToken.identifier))
            assertEquals(it.permissions, token.permissions)
            assertEquals(it.routes, token.routes)
        }
    }

}