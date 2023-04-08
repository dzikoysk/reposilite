/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.maven.application.MavenSettings
import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.shared.ErrorResponse
import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.specification.RemoteSpecificationJunitExtension
import com.reposilite.storage.api.DocumentInfo
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.UNAUTHORIZED
import kong.unirest.HeaderNames.CONTENT_LENGTH
import kong.unirest.Unirest.delete
import kong.unirest.Unirest.get
import kong.unirest.Unirest.head
import kong.unirest.Unirest.put
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalMavenIntegrationTest : MavenIntegrationTest()

@ExtendWith(RemoteSpecificationJunitExtension::class)
internal class RemoteMavenIntegrationTest : MavenIntegrationTest()

internal abstract class MavenIntegrationTest : MavenIntegrationSpecification() {

    @Test
    fun `should support head requests`() {
        // given: the details about an existing in repository file
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content", true)

        // when: client requests head data
        val response = head("$base/$repository/$gav/$file").asEmpty()

        // then: service returns valid file metadata
        assertThat(response.isSuccess).isTrue
        assertThat(response.headers.getFirst(CONTENT_LENGTH).toInt()).isEqualTo(content.length)
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

    @Test
    fun `should proxy remote file`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/remote.jar", "content") { gav, content ->
            // when: non-existing file is requested
            val notFoundResponse = get("$base/proxied/not/found.jar").asString()

            // then: service responds with 404 status page
            assertThat(notFoundResponse.isSuccess).isFalse

            // when: file that exists in remote repository is requested
            val response = get("$base/proxied/$gav").asString()

            // then: service responds with its content
            assertThat(response.body).isEqualTo(content)
            assertThat(response.isSuccess).isTrue
        }
    }

    @Test
    fun `should not proxy file with forbidden extension`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/remote.file", "content") { gav, _ ->
            // when: file that exists in remote repository is requested
            val response = get("$base/proxied/$gav").asString()
            // then: service responds with 404 status page as .file extension is not allowed
            assertThat(response.isSuccess).isFalse
        }
    }

}