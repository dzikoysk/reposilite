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

import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.HeaderNames.CONTENT_LENGTH
import kong.unirest.Unirest.delete
import kong.unirest.Unirest.get
import kong.unirest.Unirest.head
import kong.unirest.Unirest.put
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

internal abstract class MavenIntegrationTest : MavenIntegrationSpecification() {

    @Test
    fun `should support head requests`() {
        // given: the details about an existing in repository file
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content", true)

        // when: client requests head data
        val response = head("$base/$repository/$gav/$file").asEmpty()

        // then: service returns valid file metadata
        assertTrue(response.isSuccess)
        assertEquals(content.length, response.headers.getFirst(CONTENT_LENGTH).toInt())
    }

    @Test
    fun `should respond with requested file`() {
        // given: the details about an existing in repository file
        val (repository, gav, file, content) = useDocument("releases", "gav", "artifact.jar", "content", true)

        // when: client requests head data
        val response = get("$base/$repository/$gav/$file").asString()

        // then: service returns content of requested file
        assertTrue(response.isSuccess)
        assertEquals(content, response.body)
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
        assertEquals(UNAUTHORIZED.status, response.status)
    }

    @Test
    fun `should accept deploy request with valid credentials` () {
        val calls = reposilite.localConfiguration.webThreadPool.get() * 3
        val completed = CountDownLatch(calls)

        repeat(calls) { idx ->
            CompletableFuture.runAsync {
                try {
                    // given: file to upload and valid credentials
                    val (repository, gav, file) = useDocument("releases", "com/reposilite", "$idx.jar")
                    val (name, secret) = usePredefinedTemporaryAuth()
                    val (content, length) = useFile(file, 8)

                    // when: client wants to upload artifact
                    val response = put("$base/$repository/$gav/$file")
                        .body(content.inputStream())
                        .basicAuth(name, secret)
                        .asObject(DocumentInfo::class.java)

                    // then: service properly accepts connection and deploys file
                    assertTrue(response.isSuccess)
                    assertEquals(file, response.body.name)
                    assertEquals(length, response.body.contentLength)
                    assertTrue(get("$base/$repository/$gav/$file").asString().isSuccess)
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
        assertFalse(response.isSuccess)
        assertTrue(get(address).asEmpty().isSuccess)
    }

    @Test
    fun `should accept delete request with valid credentials`() {
        // given: the details about an existing in repository file
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val address = "$base/$repository/$gav/$file"
        val (name, secret) = usePredefinedTemporaryAuth()

        // when: unauthorized client tries to delete existing file
        val response = delete(address)
            .basicAuth(name, secret)
            .asString()

        // then: service rejects request and file still exists
        assertTrue(response.isSuccess)
        assertFalse(get(address).asEmpty().isSuccess)
    }

    @Test
    fun `should respond with custom 404 page`() {
        // given: an address to the non-existing resource
        val address = "$base/unknown-repository/unknown-gav/unknown-file"

        // when: unauthorized client tries to delete existing file
        val response = get(address).asString()

        // then: service responds with custom 404 page
        assertEquals(NOT_FOUND.status, response.status)
        assertTrue(response.body.contains("Reposilite - 404 Not Found"))
    }

    @Test
    fun `should proxy remote file`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/remote.file", "content") { gav, content ->
            // when: non-existing file is requested
            val notFoundResponse = get("$base/proxied/not/found.file").asString()

            // then: service responds with 404 status page
            assertFalse(notFoundResponse.isSuccess)

            // when: file that exists in remote repository is requested
            val response = get("$base/proxied/$gav").asString()

            // then: service responds with its content
            assertTrue(response.isSuccess)
            assertEquals(content, response.body)
        }
    }

}