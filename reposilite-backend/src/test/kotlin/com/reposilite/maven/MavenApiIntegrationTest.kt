package com.reposilite.maven

import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.token.api.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal abstract class MavenApiIntegrationTest : MavenIntegrationSpecification() {

    @ValueSource(strings = [
        "/api/maven/details/private",
        "/api/maven/details/private/gav",
        "/api/maven/details/private/gav/artifact.jar",
    ])
    @ParameterizedTest
    fun `should respond with protected file details only for authenticated requests`(endpoint: String) = runBlocking {
        // given: a private repository with some artifact
        useDocument("private", "gav", "artifact.jar", store = true)

        // when: user requests private resource without valid credentials
        val unauthorizedResponse = get("$base$endpoint")
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: valid credentials
        val (name, secret) = useAuth("name", "secret", routes = mapOf(endpoint.replace("/api/maven/details", "") to READ))

        // when: user requests private resource with valid credentials
        val response = get("$base$endpoint")
            .basicAuth(name, secret)
            .asString()

        // then: service responds with file details
        assertTrue(response.isSuccess)
    }

    @ValueSource(strings = [
        "api/maven/latest",
        "api/maven/versions",
    ])
    @ParameterizedTest
    fun `should find latest version`(endpoint: String) = runBlocking {
        // given: a path to the existing artifact
        useMetadata("private", "com", "reposilite", versions = listOf("1.0.1", "1.0.2", "1.0.3"))
        val artifactPath = "private/com/reposilite"
        val apiPath = "$base/$endpoint/$artifactPath"

        // when: user requests the latest version with invalid credentials
        val unauthorizedResponse = get(apiPath)
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        val (name, secret) = useAuth("name", "secret", routes = mapOf("/$artifactPath" to READ))

        // when: user requests the latest version with invalid credentials
        val response = get(apiPath)
            .basicAuth(name, secret)
            .asString()

        // then: the request should succeed
        assertTrue(response.isSuccess)
    }

}