/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.maven

import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.maven.api.RepositoryVisibility.HIDDEN
import com.reposilite.maven.api.RepositoryVisibility.PRIVATE
import com.reposilite.maven.api.RepositoryVisibility.PUBLIC
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.shared.fs.FileType.FILE
import com.reposilite.token.api.RoutePermission.READ
import com.reposilite.token.api.RoutePermission.WRITE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class MavenFacadeTest : MavenSpecification() {

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
            proxied = mutableListOf(
                "$REMOTE_REPOSITORY --store --auth $REMOTE_AUTH",
                "$REMOTE_REPOSITORY_WITH_WHITELIST --allow=do.allow"
            )
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
        val deployResult = mavenFacade.deployFile(DeployRequest(fileSpec.repository, fileSpec.gav, by, fileSpec.content.byteInputStream()))

        // then: file has been successfully stored
        assertOk(deployResult)

        // when: the deployed file is requested
        val accessToken = createAccessToken("name", "secret", fileSpec.repository, "/", READ)
        val deployedFileResult = mavenFacade.findDetails(LookupRequest(accessToken, fileSpec.repository, fileSpec.gav))

        // then: the result file matches deployed file
        val deployedFile = assertOk(deployedFileResult)
        assertEquals(FILE, deployedFile.type)
        assertEquals("reposilite-3.0.0.jar", deployedFile.name)
    }

    @ParameterizedTest
    @EnumSource(value = RepositoryVisibility::class, names = [ "PUBLIC", "HIDDEN" ])
    fun `should find requested details without credentials in public and hidden repositories`(visibility: RepositoryVisibility) = runBlocking {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(visibility.name, "/gav/file.pom", "content"))

        // when: the given file is requested
        val detailsResult = mavenFacade.findDetails(fileSpec.toLookupRequest(UNAUTHORIZED))

        // then: result is a proper file
        val fileDetails = assertOk(detailsResult)
        assertEquals("file.pom", fileDetails.name)
        assertEquals(FILE, fileDetails.type)
    }

    @Test
    fun `should require authentication to access file in private repository`(): Unit = runBlocking {
        // given: a repository with file and request without credentials
        val repository = PRIVATE.name
        val fileSpec = addFileToRepository(FileSpec(repository, "/gav/file.pom", "content"))
        var authentication = UNAUTHORIZED

        // when: the given file is requested
        val errorResponse = mavenFacade.findDetails(fileSpec.toLookupRequest(authentication))

        // then: the response contains error
        assertError(errorResponse)

        // given: access token with access to the repository
        authentication = createAccessToken("name", "secret", repository, "", READ)

        // when: the given file is requested with valid credentials
        val fileDetails = mavenFacade.findDetails(fileSpec.toLookupRequest(authentication))

        // then: response contains file details
        assertOk(fileDetails)
    }

    @ParameterizedTest
    @EnumSource(value = RepositoryVisibility::class, names = [ "HIDDEN", "PRIVATE" ])
    fun `should restrict directory indexing in hidden and private repositories `(visibility: RepositoryVisibility): Unit = runBlocking {
        // given: a repository with a file
        val fileSpec = addFileToRepository(FileSpec(visibility.name, "/gav/file.pom", "content"))

        // when: the given directory is requested
        val directoryInfo = mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, fileSpec.repository, "gav"))

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
        val data = assertOk(response)
        assertEquals(REMOTE_CONTENT, data.readBytes().decodeToString())
    }

    @Test
    fun `should find latest version` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("1.0.1", "1.0.2", "1.0.0"))))

        // when: latest version is requested
        val response = mavenFacade.findLatest(VersionLookupRequest(UNAUTHORIZED, repository, artifact, null))

        // then: should return the latest version
        assertOk("1.0.2", response)
        // assertArrayEquals(arrayOf("1.0.0", "1.0.1"), response.get())
    }

    @Test
    fun `should find latest version with specific prefix` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("2.0.1", "1.0.1", "1.0.2", "1.0.0", "2.0.0", "1.1.0"))))
        val filter = "1.0."

        // when: latest version that starts with "1.0." is requested
        val response = mavenFacade.findLatest(VersionLookupRequest(UNAUTHORIZED, repository, artifact, filter))

        // then: should return the latest version that starts with "1.0."
        assertOk("1.0.2", response)
    }

    @Test
    fun `should find all versions of the given artifact` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("1.0.1", "1.0.2", "1.0.0"))))

        // when: versions of the artifact are requested
        val response = mavenFacade.findVersions(VersionLookupRequest(UNAUTHORIZED, repository, artifact, null))

        // then: should return all versions of the artifact
        assertOk(listOf("1.0.0", "1.0.1", "1.0.2"), response)
    }

    @Test
    fun `should find all versions with specific prefix of the given artifact` () {
        // given: an artifact with metadata file
        val repository = PUBLIC.name
        val artifact = "/gav"
        mavenFacade.saveMetadata(repository, artifact, Metadata(versioning = Versioning(_versions = listOf("2.0.1", "1.0.1", "1.0.2", "1.0.0", "2.0.0", "1.1.0"))))
        val filter = "1.0."

        // when: versions that start with "1.0." of the artifact are requested
        val response = mavenFacade.findVersions(VersionLookupRequest(UNAUTHORIZED, repository, artifact, filter))

        // then: should return all versions that start with "1.0." of the artifact
        assertOk(listOf("1.0.0", "1.0.1", "1.0.2"), response)
    }

    @Test
    fun `should not find an artifact that is not allowed` () {
        // given: an artifact that is available, but not allowed
        val file = FileSpec("PROXIED", "/dont/allow", REMOTE_CONTENT)

        // when: the file is requested
        val response = mavenFacade.findFile(file.toLookupRequest(UNAUTHORIZED))

        // then: no file is found
        assertTrue(response.isErr)
    }

    @Test
    fun `should find allowed artifact in remote repository` () {
        // given: an artifact that is  both available and allowed
        val file = FileSpec("PROXIED", "/do/allow", REMOTE_CONTENT)

        // when: the file is requested
        val response = mavenFacade.findFile(file.toLookupRequest(UNAUTHORIZED))

        // then: the file is found
        val data = assertOk(response)
        assertEquals(REMOTE_CONTENT, data.readBytes().decodeToString())
    }

}