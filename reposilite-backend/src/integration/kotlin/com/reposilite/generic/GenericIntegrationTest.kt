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
        val content = "plain generic content"
        val address = "$base/files/releases/application.tar.gz"
        val (name, secret) = useDefaultManagementToken()

        val deployResponse = put(address)
            .basicAuth(name, secret)
            .body(content)
            .asEmpty()

        assertThat(deployResponse.isSuccess).isTrue
        assertThat(get(address).asString().body).isEqualTo(content)

        val headResponse = head(address).asEmpty()
        assertThat(headResponse.isSuccess).isTrue
        assertThat(headResponse.headers.getFirst(CONTENT_LENGTH).toLong()).isEqualTo(content.length.toLong())
    }

    @Test
    fun `should browse directories`() {
        val (name, secret) = useDefaultManagementToken()
        put("$base/files/releases/application.zip")
            .basicAuth(name, secret)
            .body("content")
            .asEmpty()

        val response = get("$base/files/releases").asString()

        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("application.zip")
    }

    @Test
    fun `should browse repository root`() {
        val (name, secret) = useDefaultManagementToken()
        put("$base/files/releases/application.zip")
            .basicAuth(name, secret)
            .body("content")
            .asEmpty()

        val response = get("$base/files").asString()

        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("releases")
    }

    @Test
    fun `should render missing files as html`() {
        val response = get("$base/files/missing.txt").asString()

        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.headers.getFirst(CONTENT_TYPE)).startsWith("text/html")
        assertThat(response.body).contains("Reposilite - 404 Not Found")
    }

    @Test
    fun `should reject unauthenticated writes`() {
        val response = put("$base/files/releases/application.zip")
            .body("content")
            .asObject(ErrorResponse::class.java)

        assertThat(response.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should apply shared visibility rules`() {
        val (name, secret) = useDefaultManagementToken()
        put("$base/private-files/private.txt").basicAuth(name, secret).body("private").asEmpty()
        put("$base/hidden-files/directory/hidden.txt").basicAuth(name, secret).body("hidden").asEmpty()

        assertThat(get("$base/private-files/private.txt").asEmpty().status).isEqualTo(UNAUTHORIZED.code)
        assertThat(
            get("$base/private-files/private.txt").basicAuth(name, secret).asString().body
        ).isEqualTo("private")
        assertThat(get("$base/hidden-files/directory/hidden.txt").asString().body).isEqualTo("hidden")
        assertThat(get("$base/hidden-files/directory").asEmpty().status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should enforce redeployment setting`() {
        val address = "$base/immutable-files/releases/application.zip"
        val (name, secret) = useDefaultManagementToken()

        assertThat(
            put(address).basicAuth(name, secret).body("first").asEmpty().isSuccess
        ).isTrue

        val response = put(address)
            .basicAuth(name, secret)
            .body("second")
            .asObject(ErrorResponse::class.java)

        assertThat(response.status).isEqualTo(CONFLICT.code)
        assertThat(get(address).asString().body).isEqualTo("first")
    }

    @Test
    fun `should skip invalid configurations and hide repository name conflicts`() {
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

        val genericFacade = useFacade<GenericFacade>()
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
        val address = "$base/files/releases/application.zip"
        val (name, secret) = useDefaultManagementToken()
        put(address).basicAuth(name, secret).body("content").asEmpty()

        val deleteResponse = delete(address)
            .basicAuth(name, secret)
            .asEmpty()

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
        val (name, secret) = useDefaultManagementToken()
        val (content, _) = useFile("too-large.bin", 2)

        val response = put("$base/quota-files/too-large.bin")
            .basicAuth(name, secret)
            .body(content.inputStream())
            .asObject(ErrorResponse::class.java)

        assertThat(response.status).isEqualTo(INSUFFICIENT_STORAGE.code)
    }
}
