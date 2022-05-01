package com.reposilite.settings

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.reposilite.LocalSpecificationJunitExtension
import com.reposilite.RemoteSpecificationJunitExtension
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.settings.specification.SettingsIntegrationSpecification
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.OK
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.ObjectMapper
import kong.unirest.Unirest
import kong.unirest.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalSettingsIntegrationTest : SettingsIntegrationTest()

@ExtendWith(RemoteSpecificationJunitExtension::class)
internal class RemoteSettingsIntegrationTest : SettingsIntegrationTest()

internal abstract class SettingsIntegrationTest : SettingsIntegrationSpecification() {

    companion object {
        private val DEFAULT_DOMAINS = setOf("web", "authentication", "statistics", "frontend", "maven")
    }

    @Test
    fun `should list all built-in config domains`() {
        // given: an existing token without management permission
        val (unauthorizedToken, unauthorizedSecret) = useAuth("unauthorized-token", "secret")

        // when: list of tokens is requested without valid access token
        val unauthorizedResponse = Unirest.get("$base/api/settings/domains")
            .basicAuth(unauthorizedToken, unauthorizedSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: request is rejected
        assertErrorResponse(UNAUTHORIZED, unauthorizedResponse)

        // given: a permitted token
        val (permittedName, permittedSecret) = useDefaultManagementToken()

        // when: list of tokens is requested with valid token
        val response = Unirest.get("$base/api/settings/domains")
            .basicAuth(permittedName, permittedSecret)
            .asJacksonObject(Array<String>::class)

        // then: response contains list of all
        assertSuccessResponse(OK, response) { domains ->
            assertEquals(DEFAULT_DOMAINS, domains.toSet())
        }
    }

    @Test
    fun `should offer schemes for all domains`() {
        // given: an existing token without management permission
        val (unauthorizedToken, unauthorizedSecret) = useAuth("unauthorized-token", "secret")

        DEFAULT_DOMAINS.forEach { domain ->
            // when: list of tokens is requested without valid access token
            val unauthorizedResponse = Unirest.get("$base/api/settings/schema/$domain")
                .basicAuth(unauthorizedToken, unauthorizedSecret)
                .asJacksonObject(ErrorResponse::class)

            // then: request is rejected
            assertErrorResponse(UNAUTHORIZED, unauthorizedResponse)

            // given: a permitted token
            val (permittedName, permittedSecret) = useDefaultManagementToken()

            // when: list of tokens is requested with valid token
            val response = Unirest.get("$base/api/settings/schema/$domain")
                .basicAuth(permittedName, permittedSecret)
                .asString()

            // then: response contains list of all
            assertSuccessResponse(OK, response) {
                val schema = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.readTree(it)
                assertTrue(schema.has("title"))
                assertTrue(schema.has("description"))
            }
        }
    }

    @Test
    fun `should `() {

    }

}