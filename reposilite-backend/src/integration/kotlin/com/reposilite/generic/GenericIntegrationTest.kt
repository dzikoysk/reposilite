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

package com.reposilite.generic

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.generic.application.GenericRepositorySettings
import com.reposilite.generic.application.GenericSettings
import com.reposilite.generic.specification.GenericIntegrationSpecification
import com.reposilite.repository.api.RepositoryVisibility.HIDDEN
import com.reposilite.repository.api.RepositoryVisibility.PRIVATE
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import com.reposilite.storage.s3.S3StorageProviderSettings
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.UNAUTHORIZED
import kong.unirest.core.HeaderNames.CONTENT_LENGTH
import kong.unirest.core.HeaderNames.CONTENT_TYPE
import kong.unirest.core.Unirest.delete
import kong.unirest.core.Unirest.get
import kong.unirest.core.Unirest.head
import kong.unirest.core.Unirest.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalGenericIntegrationTest : GenericIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteGenericIntegrationTest : GenericIntegrationTest() {

    @Test
    fun `should retain files when renaming a repository with a fixed S3 prefix`() {
        // given: a repository whose storage path does not depend on its name
        genericSettings.update {
            GenericSettings(
                repositories = listOf(
                    GenericRepositorySettings(
                        id = "downloads",
                        storageProvider = useTargetStorageSettings<S3StorageProviderSettings>().copy(
                            prefix = "downloads",
                            sharedBucket = false,
                        ),
                    )
                )
            )
        }
        useGenericFile("downloads", "file.txt", "content")

        // when: the repository is renamed without changing its storage
        genericSettings.update { configuration ->
            configuration.copy(repositories = configuration.repositories.map { it.copy(id = "renamed") })
        }
        val response = get("$base/renamed/file.txt").asString()
        val previousResponse = get("$base/downloads/file.txt").asEmpty()

        // then: the existing file is immediately available under the new name
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).isEqualTo("content")
        assertThat(previousResponse.status).isEqualTo(NOT_FOUND.code)
    }
}

internal abstract class GenericIntegrationTest : GenericIntegrationSpecification() {

    override fun repositories(): List<GenericRepositorySettings> =
        listOf(
            GenericRepositorySettings(id = "files", redeployment = true, storageProvider = useTargetStorageSettings()),
            GenericRepositorySettings(id = "immutable-files", redeployment = false, storageProvider = useTargetStorageSettings()),
            GenericRepositorySettings(id = "private-files", visibility = PRIVATE, redeployment = true, storageProvider = useTargetStorageSettings()),
            GenericRepositorySettings(id = "hidden-files", visibility = HIDDEN, redeployment = true, storageProvider = useTargetStorageSettings()),
        )

    @Test
    fun `should deploy and retrieve arbitrary files`() {
        // given: arbitrary content and valid management credentials
        val content = "plain generic content"
        val address = "$base/files/releases/application.tar.gz"
        val (name, secret) = useDefaultManagementToken()

        // when: content is deployed
        val deployResponse = put(address)
            .basicAuth(name, secret)
            .body(content)
            .asEmpty()

        // then: the upload succeeds
        assertThat(deployResponse.isSuccess).isTrue

        // when: the file is downloaded
        val response = get(address).asString()

        // then: the original content is returned
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).isEqualTo(content)
    }

    @Test
    fun `should return file size for head requests`() {
        // given: a file in the repository
        val content = "plain generic content"
        val address = useGenericFile("files", "releases/application.tar.gz", content)

        // when: metadata is requested without downloading the file
        val response = head(address).asEmpty()

        // then: the response contains the file size
        assertThat(response.isSuccess).isTrue
        assertThat(response.headers.getFirst(CONTENT_LENGTH).toLong()).isEqualTo(content.length.toLong())
    }

    @Test
    fun `should browse directories`() {
        // given: a file deployed in a nested directory
        useGenericFile("files", "releases/application.zip", "content")

        // when: the directory is requested
        val response = get("$base/files/releases").asString()

        // then: the index contains the deployed file
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("application.zip")
    }

    @Test
    fun `should browse repository root`() {
        // given: a file deployed below the repository root
        useGenericFile("files", "releases/application.zip", "content")

        // when: the repository root is requested
        val response = get("$base/files").asString()

        // then: the index contains the top-level directory
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("releases")
    }

    @Test
    fun `should render missing files as html`() {
        // given: an address of a missing file
        val address = "$base/files/missing.txt"

        // when: the file is requested
        val response = get(address).asString()

        // then: the response contains the HTML not-found page
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.headers.getFirst(CONTENT_TYPE)).startsWith("text/html")
        assertThat(response.body).contains("Reposilite - 404 Not Found")
    }

    @Test
    fun `should reject unauthenticated writes`() {
        // given: an address in the repository
        val address = "$base/files/releases/application.zip"

        // when: content is uploaded without credentials
        val response = put(address)
            .body("content")
            .asObject(ErrorResponse::class.java)

        // then: the request is rejected
        assertThat(response.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should require credentials to read private files`() {
        // given: a file in a private repository
        val address = useGenericFile("private-files", "private.txt", "private")
        val (name, secret) = useDefaultManagementToken()

        // when: the file is requested without credentials
        val anonymous = get(address).asEmpty()

        // then: the request is rejected
        assertThat(anonymous.status).isEqualTo(UNAUTHORIZED.code)

        // when: the file is requested with valid credentials
        val authenticated = get(address).basicAuth(name, secret).asString()

        // then: its content is returned
        assertThat(authenticated.isSuccess).isTrue
        assertThat(authenticated.body).isEqualTo("private")
    }

    @Test
    fun `should allow reading hidden files but require credentials to browse`() {
        // given: a file in a hidden repository
        val address = useGenericFile("hidden-files", "directory/hidden.txt", "hidden")

        // when: the file and its directory are requested without credentials
        val file = get(address).asString()
        val directory = get("$base/hidden-files/directory").asEmpty()

        // then: the file is readable but the directory listing is protected
        assertThat(file.isSuccess).isTrue
        assertThat(file.body).isEqualTo("hidden")
        assertThat(directory.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should enforce redeployment setting`() {
        // given: content deployed in an immutable repository
        val address = useGenericFile("immutable-files", "releases/application.zip", "first")
        val (name, secret) = useDefaultManagementToken()

        // when: new content is deployed at the same address
        val response = put(address)
            .basicAuth(name, secret)
            .body("second")
            .asObject(ErrorResponse::class.java)

        // then: redeployment is rejected and the original content remains
        assertThat(response.status).isEqualTo(CONFLICT.code)
        assertThat(get(address).asString().body).isEqualTo("first")
    }

    @ParameterizedTest
    @ValueSource(strings = ["../invalid", " invalid"])
    fun `should skip invalid configurations without losing valid repositories`(invalidName: String) {
        // given: invalid and valid repository definitions
        val invalid = GenericRepositorySettings(id = invalidName, storageProvider = useTargetStorageSettings())
        val valid = GenericRepositorySettings(id = "valid-after-invalid", storageProvider = useTargetStorageSettings())

        // when: both definitions are added
        useRepositories(invalid, valid)

        // then: only the invalid repository is skipped
        assertThat(genericFacade.getRepository(invalidName)).isNull()
        assertThat(genericFacade.getRepository("valid-after-invalid")).isNotNull()
        assertThat(genericFacade.getRepository("files")).isNotNull()
    }

    @Test
    fun `should skip repositories with duplicate names in settings`() {
        // given: a repository definition supplied twice
        val repository = GenericRepositorySettings(id = "duplicated", storageProvider = useTargetStorageSettings())

        // when: the duplicate definitions are added
        useRepositories(repository, repository)

        // then: neither duplicate is initialized and existing repositories remain available
        assertThat(genericFacade.getRepository("duplicated")).isNull()
        assertThat(genericFacade.getRepository("files")).isNotNull()
    }

    @Test
    fun `should hide repositories with names shared by another type`() {
        // given: a Maven repository and a generic definition using the same name but separate storage
        assertThat(mavenFacade.getRepository("releases")).isNotNull()
        val repository = GenericRepositorySettings(
            id = "releases",
            storageProvider = FileSystemStorageProviderSettings(mount = "generic-name-conflict"),
        )

        // when: the generic repository is added and the shared address is requested
        useRepositories(repository)
        val response = get("$base/releases").asEmpty()

        // then: both repositories exist but neither is exposed at the ambiguous address
        assertThat(genericFacade.getRepository("releases")).isNotNull()
        assertThat(mavenFacade.getRepository("releases")).isNotNull()
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
    }

    @Test
    fun `should delete files`() {
        // given: a successfully deployed file and valid credentials
        val address = useGenericFile("files", "releases/application.zip", "content")
        val (name, secret) = useDefaultManagementToken()

        // when: the file is deleted
        val response = delete(address).basicAuth(name, secret).asEmpty()

        // then: deletion succeeds and the file is no longer available
        assertThat(response.isSuccess).isTrue
        assertThat(get(address).asEmpty().status).isEqualTo(NOT_FOUND.code)
    }

}
