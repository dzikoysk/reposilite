package com.reposilite.maven

import com.reposilite.ReposiliteSpec
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MavenIntegrationTest : ReposiliteSpec() {

    @Test
    fun `should reject unauthorized deploy request`() {
        val response = put("$base/releases/gav/artifact.jar")
            .asObject(ErrorResponse::class.java)
            .body

        assertEquals(UNAUTHORIZED.status, response.status)
    }

    @Test
    fun `should accept deploy request with valid credentials` () {
        val response = put("$base/releases/gav/artifact.jar")
            .basicAuth(NAME, SECRET)
            .asObject(DocumentInfo::class.java)

        assertTrue(response.isSuccess)
        assertEquals("artifact.jar", response.body.name)
    }

}