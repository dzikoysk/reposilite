package com.reposilite.maven

import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.spec.MavenIntegrationSpec
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.HeaderNames.CONTENT_LENGTH
import kong.unirest.Unirest.delete
import kong.unirest.Unirest.get
import kong.unirest.Unirest.head
import kong.unirest.Unirest.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.channels.Channels

internal class MavenIntegrationTest : MavenIntegrationSpec() {

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
            .body

        // then: service should reject the request
        assertEquals(UNAUTHORIZED.status, response.status)
    }

    @Test
    fun `should accept deploy request with valid credentials` () {
        // given: file to upload and valid credentials
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar")
        val (name, secret) = useAuth()
        val content = useFile(file, 100)

        try {
            // when: client wants to upload artifact
            val response = put("$base/$repository/$gav/$file")
                .body(Channels.newInputStream(content.channel).readBytes()) // move to InputStream when: https://github.com/Kong/unirest-java/issues/411
                .basicAuth(name, secret)
                .asObject(DocumentInfo::class.java)

            // then: service properly accepts connection and deploys file
            assertTrue(response.isSuccess)
            assertEquals(file, response.body.name)
            assertEquals(content.length(), response.body.contentLength)
        } finally {
            content.channel.close()
        }
    }

    @Test
    fun `should reject unauthorized delete request`() {
        // given: the details about an existing in repository file
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val address = "$base/$repository/$gav/$file"

        // when: unauthorized client tries to delete existing file
        val response = delete(address)
            .basicAuth("name", "invalid-secret")
            .asString()

        // then: service rejects request and file still exists
        assertFalse(response.isSuccess)
        assertTrue(get(address).asEmpty().isSuccess)
    }

    @Test
    fun `should accept delete request with valid credentials`() {
        // given: the details about an existing in repository file
        val (repository, gav, file) = useDocument("releases", "gav", "artifact.jar", "content", true)
        val address = "$base/$repository/$gav/$file"
        val (name, secret) = useAuth()

        // when: unauthorized client tries to delete existing file
        val response = delete(address)
            .basicAuth(name, secret)
            .asString()

        // then: service rejects request and file still exists
        assertTrue(response.isSuccess)
        assertFalse(get(address).asEmpty().isSuccess)
    }

}