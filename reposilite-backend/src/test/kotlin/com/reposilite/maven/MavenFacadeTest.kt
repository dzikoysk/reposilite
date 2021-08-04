package com.reposilite.maven

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import com.reposilite.config.Configuration.RepositoryConfiguration
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.shared.FileType.FILE
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission.READ
import panda.utilities.IOUtils

internal class MavenFacadeTest : MavenSpec() {

    override fun repositories() = mapOf(
        "public" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.PUBLIC
        },
        "hidden" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.HIDDEN
        },
        "private" to RepositoryConfiguration().also {
            it.visibility = RepositoryVisibility.PRIVATE
        }
    )

    @ParameterizedTest
    @EnumSource(RepositoryVisibility::class)
    fun `should deploy file under the given path`(visibility: RepositoryVisibility) {
        // given: an uri and file to store
        val fileSpec = FileSpec(visibility.name.lowercase(), "/org/panda-lang/reposilite/3.0.0/reposilite-3.0.0.jar", "content")
        val by = "dzikoysk@127.0.0.1"

        // when: the following file is deployed
        val fileDetails = mavenFacade.deployFile(DeployRequest(fileSpec.repository, fileSpec.gav, by, fileSpec.content.byteInputStream()))
            .orElseThrow { fail { it.toString() } }

        // then: file has been successfully stored
        assertEquals(FILE, fileDetails.type)
        assertEquals("reposilite-3.0.0.jar", fileDetails.name)
        assertFalse(fileDetails.isReadable())
    }

    @ParameterizedTest
    @EnumSource(value = RepositoryVisibility::class, names = [ "PUBLIC", "HIDDEN" ])
    fun `should find requested file without credentials in public and hidden repository`(visibility: RepositoryVisibility) {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(visibility.name.lowercase(), "gav/file.pom", "content"))
        val authentication = null

        // when: the given file is requested
        val fileDetails = mavenFacade.findFile(fileSpec.toLookupRequest(authentication))
            .orElseThrow { fail { it.toString() } }

        // then: result is a proper file
        assertEquals("file.pom", fileDetails.name)
        assertTrue(fileDetails.isReadable())
        assertEquals(FILE, fileDetails.type)
        val documentInfo = fileDetails as DocumentInfo
        assertEquals(fileSpec.content, IOUtils.convertStreamToString(documentInfo.content()).get())
    }

    @Test
    fun `should require authentication to access file in private repository`() {
        // given: a repository with file and request without credentials
        val repository = RepositoryVisibility.PRIVATE.name.lowercase()
        val fileSpec = addFileToRepository(FileSpec(repository, "gav/file.pom", "content"))
        var authentication: AccessToken? = null

        // when: the given file is requested
        val errorResponse = mavenFacade.findFile(fileSpec.toLookupRequest(authentication))

        // then: the response contains error
        assertTrue(errorResponse.isErr)

        // given: access token with access to the repository
        val routes = setOf(Route("/$repository", setOf(READ)))
        authentication = AccessToken(alias = "alias", secret = "secret", routes = routes)

        // when: the given file is requested with valid credentials
        val fileDetails = mavenFacade.findFile(fileSpec.toLookupRequest(authentication))

        // then: response contains file details
        assertTrue(fileDetails.isOk)
    }

}