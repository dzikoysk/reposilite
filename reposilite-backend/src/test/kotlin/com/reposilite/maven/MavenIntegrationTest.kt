package com.reposilite.maven

import com.reposilite.ReposiliteSpec
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MavenIntegrationTest : ReposiliteSpec() {

    @Test
    fun `should reject unauthorized deploy request`() {
        val response = put("$base/api/maven/details/releases/gav/artifact.jar")
            .asObject(ErrorResponse::class.java)
            .body

        assertEquals(UNAUTHORIZED.status, response.status)
    }

}