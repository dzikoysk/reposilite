package com.reposilite.maven

import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.maven.api.RepositoryVisibility.HIDDEN
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.maven.api.RepositoryVisibility.PUBLIC
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.spec.MavenSpec
import com.reposilite.shared.FileType.FILE
import com.reposilite.token.api.RoutePermission.READ
import com.reposilite.token.api.RoutePermission.WRITE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import panda.utilities.IOUtils

internal class MavenFacadeTest : MavenSpec() {

    override fun repositories() = linkedMapOf(
        createRepository(PRIVATE.name) {
            visibility = PRIVATE
        },
        createRepository(HIDDEN.name) {
            visibility = HIDDEN
        },
        createRepository(PUBLIC.name) {
            visibility = PUBLIC
        },
        createRepository("PROXIED") {
            visibility = PUBLIC
            proxied = mutableListOf("$REMOTE_REPOSITORY --store --auth $REMOTE_AUTH")
        }
    )

    @Test
    fun `should list available repositories`() = runBlocking {
        // when: repositories are requested without any credentials
        var availableRepositories = findRepositories(UNAUTHORIZED)

        // then: response contains only public repositories
        assertEquals(listOf(PUBLIC.name, "PROXIED"), availableRepositories)

        // given: a token with access to private repository
        val accessToken = createAccessToken("name", "secret", PRIVATE.name, "gav", WRITE)

        // when: repositories are requested with valid credentials
        availableRepositories = findRepositories(accessToken)

        // then: response contains authorized repositories
        assertEquals(listOf(PRIVATE.name, PUBLIC.name, "PROXIED"), availableRepositories)
    }

    @ParameterizedTest
    @EnumSource(RepositoryVisibility::class)
    fun `should deploy file under the given path`(visibility: RepositoryVisibility) {
        // given: an uri and file to store
        val fileSpec = FileSpec(visibility.name, "/com/reposilite/3.0.0/reposilite-3.0.0.jar", "content")
        val by = "dzikoysk@127.0.0.1"

        // when: the following file is deployed
        val fileDetails = mavenFacade.deployFile(DeployRequest(fileSpec.repository, fileSpec.gav, by, fileSpec.content.byteInputStream()))
            .orElseThrow { fail { it.toString() } }

        // then: file has been successfully stored
        assertEquals(FILE, fileDetails.type)
        assertEquals("reposilite-3.0.0.jar", fileDetails.name)
    }

    @ParameterizedTest
    @EnumSource(value = RepositoryVisibility::class, names = [ "PUBLIC", "HIDDEN" ])
    fun `should find requested file without credentials in public and hidden repositories`(visibility: RepositoryVisibility) = runBlocking {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(visibility.name, "/gav/file.pom", "content"))

        // when: the given file is requested
        val fileDetails = mavenFacade.findFile(fileSpec.toLookupRequest(UNAUTHORIZED))
            .orElseThrow { fail { it.toString() } }

        // then: result is a proper file
        assertEquals("file.pom", fileDetails.name)
        // assertTrue(fileDetails.isReadable())
        assertEquals(FILE, fileDetails.type)
        val documentInfo = fileDetails as DocumentInfo
        assertEquals(fileSpec.content, IOUtils.convertStreamToString(documentInfo.content()).get())
    }

    @Test
    fun `should require authentication to access file in private repository`(): Unit = runBlocking {
        // given: a repository with file and request without credentials
        val repository = PRIVATE.name
        val fileSpec = addFileToRepository(FileSpec(repository, "/gav/file.pom", "content"))
        var authentication = UNAUTHORIZED

        // when: the given file is requested
        val errorResponse = mavenFacade.findFile(fileSpec.toLookupRequest(authentication))

        // then: the response contains error
        assertError(errorResponse)

        // given: access token with access to the repository
        authentication = createAccessToken("name", "secret", repository, "", READ)

        // when: the given file is requested with valid credentials
        val fileDetails = mavenFacade.findFile(fileSpec.toLookupRequest(authentication))

        // then: response contains file details
        assertOk(fileDetails)
    }

    @ParameterizedTest
    @EnumSource(value = RepositoryVisibility::class, names = [ "HIDDEN", "PRIVATE" ])
    fun `should restrict directory indexing in hidden and private repositories `(visibility: RepositoryVisibility): Unit = runBlocking {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(visibility.name, "/gav/file.pom", "content"))

        // when: the given directory is requested
        val directoryInfo = mavenFacade.findFile(LookupRequest(UNAUTHORIZED, fileSpec.repository, "gav"))

        // then: response contains error
        assertError(directoryInfo)
    }

    @Test
    fun `should delete file` () {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(PUBLIC.name, "/gav/file.pom", "content"))
        var authentication = createAccessToken("invalid", "invalid", "invalid", "invalid", WRITE)

        // when: the given file is deleted with invalid credentials
        val errorResponse = mavenFacade.deleteFile(DeleteRequest(authentication, fileSpec.repository, fileSpec.gav))

        // then: response contains error
        assertError(errorResponse)

        // given: a valid credentials
        authentication = createAccessToken("name", "secret", PUBLIC.name, "gav", WRITE)

        // when: the given file is deleted with valid credentials
        val response = mavenFacade.deleteFile(DeleteRequest(authentication, fileSpec.repository, fileSpec.gav))

        // then: file has been deleted
        assertOk(response)
    }

    @Test
    fun `should serve proxied file from remote host and store it in local repository` () = runBlocking {
        // given: a file available in remote repository
        val fileSpec = FileSpec("PROXIED", "/gav/file.pom", REMOTE_CONTENT)

        // when: a remote file is requested through proxied repository
        val response = mavenFacade.findFile(fileSpec.toLookupRequest(UNAUTHORIZED))

        // then: the file has been properly proxied
        assertOk(response)
        assertEquals(REMOTE_CONTENT, (response.get() as DocumentInfo).content().readBytes().decodeToString())
    }

    @Test
    fun `should find latest version` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("1.0.1", "1.0.2", "1.0.0"))))

        // when: latest version is requested
        val response = mavenFacade.findLatest(LookupRequest(UNAUTHORIZED, repository, artifact))

        // then: should return the latest version
        assertOk("1.0.2", response)
        // assertArrayEquals(arrayOf("1.0.0", "1.0.1"), response.get())
    }

    @Test
    fun `should find all versions of the given artifact` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("1.0.1", "1.0.2", "1.0.0"))))

        // when: latest version is requested
        val response = mavenFacade.findVersions(LookupRequest(UNAUTHORIZED, repository, artifact))

        // then: should return the latest version
        assertOk(listOf("1.0.0", "1.0.1", "1.0.2"), response)
    }

}