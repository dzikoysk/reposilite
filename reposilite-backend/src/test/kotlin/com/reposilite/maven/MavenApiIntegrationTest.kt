package com.reposilite.maven

import com.reposilite.maven.spec.MavenIntegrationSpec
import com.reposilite.token.api.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class MavenApiIntegrationTest : MavenIntegrationSpec() {

    @ValueSource(strings = [
        "/api/maven/details/private",
        "/api/maven/details/private/gav",
        "/api/maven/details/private/gav/artifact.jar",
    ])
    @ParameterizedTest
    fun `should respond with protected file details only for authenticated requests`(uri: String) = runBlocking {
        // given: a private repository with some artifact
        useDocument("private", "gav", "artifact.jar", store = true)

        // when: user requests private resource without valid credentials
        val unauthorizedResponse = get("$base$uri")
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: valid credentials
        val (name, secret) = useAuth("name", "secret", routes = mapOf(uri.replace("/api/maven/details", "") to READ))

        // when: user requests private resource with valid credentials
        val response = get("$base$uri")
            .basicAuth(name, secret)
            .asString()

        // then: service responds with file details
        assertTrue(response.isSuccess)
    }

}