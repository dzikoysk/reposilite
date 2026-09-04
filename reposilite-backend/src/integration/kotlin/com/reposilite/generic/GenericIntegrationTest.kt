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
import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.generic.application.GenericRepositorySettings
import com.reposilite.generic.application.GenericSettings
import com.reposilite.repository.RepositoryFacade
import com.reposilite.repository.api.RepositoryAccessMode.HIDDEN
import com.reposilite.repository.api.RepositoryAccessMode.PRIVATE
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.INSUFFICIENT_STORAGE
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

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalGenericIntegrationTest : GenericIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteGenericIntegrationTest : GenericIntegrationTest()

internal abstract class GenericIntegrationTest : ReposiliteSpecification() {

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        sharedConfigurationFacade.getDomainSettings<GenericSettings>().update {
            GenericSettings(
                repositories = listOf(
                    GenericRepositorySettings(id = "files", redeployment = true, storageProvider = _storageProvider!!),
                    GenericRepositorySettings(id = "immutable-files", redeployment = false, storageProvider = _storageProvider!!),
                    GenericRepositorySettings(id = "private-files", visibility = PRIVATE, redeployment = true, storageProvider = _storageProvider!!),
                    GenericRepositorySettings(id = "hidden-files", visibility = HIDDEN, redeployment = true, storageProvider = _storageProvider!!),
                )
            )
        }
    }

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

        // then: the content can be retrieved
        assertThat(deployResponse.isSuccess).isTrue
        assertThat(get(address).asString().body).isEqualTo(content)

        // when: metadata is requested without downloading the file
        val headResponse = head(address).asEmpty()

        // then: the response contains the file size
        assertThat(headResponse.isSuccess).isTrue
        assertThat(headResponse.headers.getFirst(CONTENT_LENGTH).toLong()).isEqualTo(content.length.toLong())
    }

    @Test
    fun `should browse directories`() {
        // given: a file deployed in a nested directory
        val (name, secret) = useDefaultManagementToken()
        put("$base/files/releases/application.zip")
            .basicAuth(name, secret)
            .body("content")
            .asEmpty()

        // when: the directory is requested
        val response = get("$base/files/releases").asString()

        // then: the index contains the deployed file
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("application.zip")
    }

    @Test
    fun `should browse repository root`() {
        // given: a file deployed below the repository root
        val (name, secret) = useDefaultManagementToken()
        put("$base/files/releases/application.zip")
            .basicAuth(name, secret)
            .body("content")
            .asEmpty()

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
        // given: a write request without credentials
        val address = "$base/files/releases/application.zip"

        // when: content is uploaded
        val response = put(address)
            .body("content")
            .asObject(ErrorResponse::class.java)

        // then: the request is rejected
        assertThat(response.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should apply shared visibility rules`() {
        // given: files in private and hidden repositories
        val (name, secret) = useDefaultManagementToken()
        put("$base/private-files/private.txt").basicAuth(name, secret).body("private").asEmpty()
        put("$base/hidden-files/directory/hidden.txt").basicAuth(name, secret).body("hidden").asEmpty()

        // when: the files and directory index are requested
        val anonymousPrivateResponse = get("$base/private-files/private.txt").asEmpty()
        val authenticatedPrivateResponse = get("$base/private-files/private.txt").basicAuth(name, secret).asString()
        val hiddenFileResponse = get("$base/hidden-files/directory/hidden.txt").asString()
        val hiddenDirectoryResponse = get("$base/hidden-files/directory").asEmpty()

        // then: shared visibility rules are enforced
        assertThat(anonymousPrivateResponse.status).isEqualTo(UNAUTHORIZED.code)
        assertThat(authenticatedPrivateResponse.body).isEqualTo("private")
        assertThat(hiddenFileResponse.body).isEqualTo("hidden")
        assertThat(hiddenDirectoryResponse.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should enforce redeployment setting`() {
        // given: content deployed in an immutable repository
        val address = "$base/immutable-files/releases/application.zip"
        val (name, secret) = useDefaultManagementToken()
        assertThat(
            put(address).basicAuth(name, secret).body("first").asEmpty().isSuccess
        ).isTrue

        // when: new content is deployed at the same address
        val response = put(address)
            .basicAuth(name, secret)
            .body("second")
            .asObject(ErrorResponse::class.java)

        // then: redeployment is rejected and the original content remains
        assertThat(response.status).isEqualTo(CONFLICT.code)
        assertThat(get(address).asString().body).isEqualTo("first")
    }

    @Test
    fun `should skip invalid configurations and hide repository name conflicts`() {
        // given: invalid, conflicting, and duplicated repository definitions
        val genericSettings = useFacade<SharedConfigurationFacade>().getDomainSettings<GenericSettings>()
        genericSettings.update { settings ->
            settings.copy(
                repositories = settings.repositories + listOf(
                    GenericRepositorySettings(id = "../invalid", storageProvider = _storageProvider!!),
                    GenericRepositorySettings(
                        id = "releases",
                        storageProvider = FileSystemStorageProviderSettings(mount = "generic-name-conflict"),
                    ),
                    GenericRepositorySettings(id = "duplicated", storageProvider = _storageProvider!!),
                    GenericRepositorySettings(id = "duplicated", storageProvider = _storageProvider!!),
                )
            )
        }

        // when: repositories are resolved after the configuration reload
        val genericFacade = useFacade<GenericFacade>()

        // then: invalid and ambiguous repositories are hidden without affecting valid repositories
        assertThat(genericFacade.getRepository("../invalid")).isNull()
        assertThat(genericFacade.getRepository("releases")).isNotNull()
        assertThat(genericFacade.getRepository("duplicated")).isNull()
        assertThat(genericFacade.getRepository("files")).isNotNull()
        assertThat(mavenFacade.getRepository("releases")).isNotNull()
        assertThat(useFacade<RepositoryFacade>().findRepository("releases")).isNull()
        assertThat(get("$base/releases").asEmpty().status).isEqualTo(NOT_FOUND.code)
    }

    @Test
    fun `should delete files`() {
        // given: a deployed file and valid management credentials
        val address = "$base/files/releases/application.zip"
        val (name, secret) = useDefaultManagementToken()
        put(address).basicAuth(name, secret).body("content").asEmpty()

        // when: the file is deleted
        val deleteResponse = delete(address)
            .basicAuth(name, secret)
            .asEmpty()

        // then: deletion succeeds and the file is no longer available
        assertThat(deleteResponse.isSuccess).isTrue
        assertThat(get(address).asEmpty().status).isEqualTo(NOT_FOUND.code)
    }
}

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class GenericQuotaIntegrationTest : ReposiliteSpecification() {

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        sharedConfigurationFacade.getDomainSettings<GenericSettings>().update {
            GenericSettings(
                repositories = listOf(
                    GenericRepositorySettings(
                        id = "quota-files",
                        storageProvider = FileSystemStorageProviderSettings(quota = "1MB"),
                    )
                )
            )
        }
    }

    @Test
    fun `should enforce storage quota`() {
        // given: content larger than the repository quota
        val (name, secret) = useDefaultManagementToken()
        val (content, _) = useFile("too-large.bin", 2)

        // when: the content is uploaded
        val response = put("$base/quota-files/too-large.bin")
            .basicAuth(name, secret)
            .body(content.inputStream())
            .asObject(ErrorResponse::class.java)

        // then: the upload is rejected because storage is insufficient
        assertThat(response.status).isEqualTo(INSUFFICIENT_STORAGE.code)
    }
}
