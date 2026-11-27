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

package com.reposilite.maven

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.maven.application.MavenSettings
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extensions.maxAge
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.s3.S3StorageProviderSettings
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.UNAUTHORIZED
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import kong.unirest.core.HeaderNames.CACHE_CONTROL
import kong.unirest.core.HeaderNames.CONTENT_LENGTH
import kong.unirest.core.Unirest.delete
import kong.unirest.core.Unirest.get
import kong.unirest.core.Unirest.head
import kong.unirest.core.Unirest.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalMavenIntegrationTest : MavenIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteMavenIntegrationTest : MavenIntegrationTest() {

    @Test
    fun `should preserve invalid repository settings and recover after correction`() {
        // given: a configuration containing an invalid repository name
        val storage = useTargetStorageSettings<S3StorageProviderSettings>()
        val configuration = MavenSettings(repositories = listOf(
            RepositorySettings(id = " invalid", storageProvider = storage),
            RepositorySettings(id = "second", storageProvider = storage),
        ))
        val (name, secret) = useDefaultManagementToken()
        val facade = useFacade<SharedConfigurationFacade>()

        // when: the invalid configuration is saved through the settings API
        val update = put("$base/api/settings/domain/maven").basicAuth(name, secret).body(configuration).asEmpty()
        val saved = get("$base/api/settings/domain/maven").basicAuth(name, secret).asObject(MavenSettings::class.java)
        val persisted = DEFAULT_OBJECT_MAPPER.readTree(facade.fetchConfiguration()).get("maven")

        // then: the full configuration is retained and only the valid repository loads
        assertThat(update.isSuccess).isTrue()
        assertThat(saved.body).isEqualTo(configuration)
        assertThat(DEFAULT_OBJECT_MAPPER.treeToValue(persisted, MavenSettings::class.java)).isEqualTo(configuration)
        assertThat(mavenFacade.getRepositories().map { it.name }).containsExactly("second")

        // when: the invalid name is corrected
        val corrected = configuration.copy(repositories = configuration.repositories.map {
            if (it.id == " invalid") it.copy(id = "first") else it
        })
        val correction = put("$base/api/settings/domain/maven").basicAuth(name, secret).body(corrected).asEmpty()

        // then: repositories become available without restarting the instance
        assertThat(correction.isSuccess).isTrue()
        assertThat(mavenFacade.getRepositories().map { it.name }).containsExactlyInAnyOrder("first", "second")
    }

}

internal abstract class MavenIntegrationTest : MavenIntegrationSpecification() {

    @Test
    fun `should authorize artifact writes using the logical filename`() {
        // given: a token scoped to one file with URL-sensitive characters
        val (name, secret) = useAuth("writer", "secret", routes = mapOf("/releases/build #1.jar" to WRITE))
        val url = "$base/releases/build%20%231.jar"

        // when: the writer deploys and deletes that artifact
        val deployment = put(url).basicAuth(name, secret).body("content").asEmpty()
        val download = get(url).asString()
        val deletion = delete(url).basicAuth(name, secret).asEmpty()

        // then: the permission and both operations identify the same artifact
        assertThat(deployment.isSuccess).isTrue
        assertThat(download.body).isEqualTo("content")
        assertThat(deletion.isSuccess).isTrue
    }

    @Test
    fun `should decode the HTTP artifact path once`() {
        // given: two distinct files in storage
        useDocument("releases", "group", "release 2.jar", "space", store = true)
        useDocument("releases", "group", "release%202.jar", "percent", store = true)

        // when: each file is requested by its encoded URL
        val space = get("$base/releases/group/release%202.jar").asString()
        val percent = get("$base/releases/group/release%25202.jar").asString()

        // then: each URL resolves to its own logical name
        assertThat(space.body).isEqualTo("space")
        assertThat(percent.body).isEqualTo("percent")
    }

    @Test
    fun `should check write permission before looking up the repository`() {
        // given: a token with write permission for one unconfigured repository
        val (name, secret) = useAuth("writer", "secret", routes = mapOf("/planned/" to WRITE))

        // when: the token requests the same operation in each repository
        val existing = delete("$base/releases/file.jar").basicAuth(name, secret).asEmpty()
        val unknown = delete("$base/unknown/file.jar").basicAuth(name, secret).asEmpty()
        val permitted = delete("$base/planned/file.jar").basicAuth(name, secret).asEmpty()

        // then: only the permitted request reaches repository lookup
        assertThat(existing.status).isEqualTo(FORBIDDEN.code)
        assertThat(unknown.status).isEqualTo(FORBIDDEN.code)
        assertThat(permitted.status).isEqualTo(NOT_FOUND.code)
    }

    @Test
    fun `should resolve a local mirror name before interpreting URL delimiters`() {
        // given: a local repository and a mirror reference with a trailing slash
        val settings = useFacade<SharedConfigurationFacade>().getDomainSettings<MavenSettings>()
        settings.update { config ->
            val releases = config.repositories.first { it.id == "releases" }
            config.copy(repositories = listOf(
                releases.copy(id = "build#1"),
                releases.copy(proxied = listOf(MirroredRepositorySettings(reference = "build#1/")))
            ))
        }
        useDocument("build#1", "group", "artifact.jar", "local content", store = true)

        // when: the artifact is requested through the mirror
        val response = get("$base/releases/group/artifact.jar").asString()

        // then: the file is read from the local repository
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).isEqualTo("local content")
    }

    @Test
    fun `should preserve stored files across repository reloads`() {
        // given: an artifact and the current storage provider
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val previous = mavenFacade.getRepository(repository)!!.storageProvider
        val settings = useFacade<SharedConfigurationFacade>().getDomainSettings<MavenSettings>()

        // when: repository settings are reloaded twice
        repeat(2) { settings.update { it.copy() } }
        val response = get("$base/$repository/$gav/$file").asString()

        // then: replacement storage providers can reuse the same storage and read its files
        assertThat(mavenFacade.getRepository(repository)!!.storageProvider).isNotSameAs(previous)
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).isEqualTo(content)
    }

    @Test
    fun `should skip invalid repository names when reloading settings`() {
        // given: invalid and valid repository definitions in the same configuration
        val settings = useFacade<SharedConfigurationFacade>().getDomainSettings<MavenSettings>()
        val configuration = settings.get().copy(
            repositories = settings.get().repositories + listOf(
                RepositorySettings(id = " invalid", storageProvider = useTargetStorageSettings()),
                RepositorySettings(id = "valid-after-invalid", storageProvider = useTargetStorageSettings()),
            ),
        )

        // when: the configuration is reloaded
        settings.update { configuration }

        // then: only the invalid repository is skipped
        assertThat(mavenFacade.getRepository(" invalid")).isNull()
        assertThat(mavenFacade.getRepository("valid-after-invalid")).isNotNull()
        assertThat(mavenFacade.getRepository("releases")).isNotNull()
    }

    @Test
    fun `should browse repository root`() {
        // given: an artifact in a repository directory
        useDocument("releases", "gav", "artifact.jar", "content", true)

        // when: the repository root is requested
        val response = get("$base/releases").asString()

        // then: the index contains the artifact directory
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("gav")
    }

    @Test
    fun `should support head requests`() {
        // given: the details about an existing in repository file
        val (repository, gav, file, content) = useDocument("immutable", "gav", "artifact.jar", "content", true)

        // when: client requests head data
        val response = head("$base/$repository/$gav/$file").asEmpty()

        // then: service returns valid file metadata
        assertThat(response.isSuccess).isTrue
        assertThat(response.headers.getFirst(CONTENT_LENGTH).toInt()).isEqualTo(content.length)
        assertThat(response.headers.getFirst(CACHE_CONTROL)).isEqualTo("public, max-age=$maxAge")
    }

    @Test
    fun `should prevent shared caching of private artifact downloads`() {
        // given: an immutable private artifact and a token with read access
        val (repository, gav, file, content) = useDocument("private-immutable", "gav", "artifact.jar", store = true)
        val (name, secret) = useAuth("reader", "secret", routes = mapOf("/$repository" to READ))
        val url = "$base/$repository/$gav/$file"

        // when: the reader requests the artifact and its headers
        val download = get(url).basicAuth(name, secret).asString()
        val headers = head(url).basicAuth(name, secret).asEmpty()

        // then: both successful responses forbid storage
        assertThat(download.isSuccess).isTrue
        assertThat(download.body).isEqualTo(content)
        assertThat(headers.isSuccess).isTrue
        assertThat(download.headers.getFirst(CACHE_CONTROL)).isEqualTo("no-cache, no-store, max-age=0")
        assertThat(headers.headers.getFirst(CACHE_CONTROL)).isEqualTo("no-cache, no-store, max-age=0")
    }

    @Test
    fun `should respond with requested file`() {
        // given: the details about an existing in repository file
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content", true)

        // when: client requests head data
        val response = get("$base/$repository/$gav/$file").asString()

        // then: service returns content of requested file
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).isEqualTo(content)
    }

    @Test
    fun `should reject unauthorized deploy request`() {
        // given: a document to upload
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content")

        // when: client tries to deploy file without valid credentials
        val response = put("$base/$repository/$gav/$file")
            .body(content)
            .basicAuth("name", "invalid-secret")
            .asObject(ErrorResponse::class.java)

        // then: service should reject the request
        assertThat(response.status).isEqualTo(UNAUTHORIZED.code)
    }

    @Test
    fun `should accept deploy request with valid credentials` () {
        val calls = useFacade<LocalConfiguration>().webThreadPool.get() * 3
        val completed = CountDownLatch(calls)

        repeat(calls) { idx ->
            CompletableFuture.runAsync {
                try {
                    // given: file to upload and valid credentials
                    val (repository, gav, file) = useDocument("releases", "com/reposilite", "$idx.jar")
                    val (name, secret) = useDefaultManagementToken()
                    val (content, length) = useFile(file, 8)

                    // when: client wants to upload artifact
                    val response = put("$base/$repository/$gav/$file")
                        .body(content.inputStream())
                        .basicAuth(name, secret)
                        .asObject(DocumentInfo::class.java)

                    // then: service properly accepts connection and deploys file
                    assertThat(response.isSuccess).isTrue
                    assertThat(response.body.name).isEqualTo(file)
                    assertThat(response.body.contentLength).isEqualTo(length)
                    assertThat(get("$base/$repository/$gav/$file").asString().isSuccess).isTrue
                } finally {
                    completed.countDown()
                }
            }
        }

        completed.await()
    }

    @Test
    fun `should reject unauthorized delete request`() {
        // given: the details about an existing in repository file
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val address = "$base/$repository/$gav/$file"

        // when: unauthorized client tries to delete existing file
        val response = delete(address)
            .basicAuth("name", "invalid-secret")
            .asObject(ErrorResponse::class.java)

        // then: service rejects request and file still exists
        assertThat(response.isSuccess).isFalse
        assertThat(get(address).asEmpty().isSuccess).isTrue
    }

    @Test
    fun `should accept delete request with valid credentials`() {
        // given: the details about an existing in repository file
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val address = "$base/$repository/$gav/$file"
        val (name, secret) = useDefaultManagementToken()

        // when: unauthorized client tries to delete existing file
        val response = delete(address)
            .basicAuth(name, secret)
            .asString()

        // then: service rejects request and file still exists
        assertThat(response.isSuccess).isTrue
        assertThat(get(address).asEmpty().isSuccess).isFalse
    }

    @Test
    fun `should respond with custom 404 page`() {
        // given: an address to the non-existing resource
        val address = "$base/unknown-repository/unknown-gav/unknown-file"

        // when: unauthorized client tries to delete existing file
        val response = get(address).asString()

        // then: service responds with custom 404 page
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.body).contains("Reposilite - 404 Not Found")
    }

}
