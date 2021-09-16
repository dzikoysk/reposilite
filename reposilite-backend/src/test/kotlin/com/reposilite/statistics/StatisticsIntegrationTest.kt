package com.reposilite.statistics

import com.reposilite.ReposiliteSpecification
import com.reposilite.token.api.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal abstract class StatisticsIntegrationTest : ReposiliteSpecification() {

    @Test
    fun `should return registered amount of endpoint calls`() = runBlocking {
        // given: a route to request and check
        val route = "/releases/com/reposilite"
        val endpoint = "$base/api/statistics/count/request$route"
        get("$base$route").asEmpty()

        // when: stats service is requested without valid credentials
        val unauthorizedResponse = get(endpoint).asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", mapOf(route to READ))

        // when: service is requested with valid credentials
        val response = get(endpoint)
            .basicAuth(name, secret)
            .asString()

        println(response.body)
        // then: service responds with valid stats data
        assertTrue(response.isSuccess)
        //assertEquals("1", response.body)
    }

}