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

package com.reposilite.configuration

import com.reposilite.ReposiliteObjectMapper
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.auth.application.LdapSettings
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.specification.SettingsIntegrationSpecification
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.maven.application.MavenSettings
import com.reposilite.shared.ErrorResponse
import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.statistics.api.ResolvedRequestsInterval.YEARLY
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.web.application.WebSettings
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.OK
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalSettingsIntegrationTest : SettingsIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteSettingsIntegrationTest : SettingsIntegrationTest()

internal abstract class SettingsIntegrationTest : SettingsIntegrationSpecification() {

    companion object {
        private val DEFAULT_DOMAINS = mapOf(
            "web" to WebSettings::class,
            "authentication" to AuthenticationSettings::class,
            "statistics" to StatisticsSettings::class,
            "frontend" to FrontendSettings::class,
            "maven" to MavenSettings::class
        )
    }

    @Test
    fun `should list all built-in config domains`() {
        // then: endpoint requires management access token
        assertManagerOnlyGetEndpoint("/api/settings/domains")

        // given: a permitted token
        val (permittedName, permittedSecret) = useDefaultManagementToken()

        // when: list of domains is requested
        val response = Unirest.get("$base/api/settings/domains")
            .basicAuth(permittedName, permittedSecret)
            .asJacksonObject(Array<String>::class)

        // then: response contains list of all of them
        assertSuccessResponse(OK, response) { domains ->
            assertThat(domains.toSet()).isEqualTo(DEFAULT_DOMAINS.keys)
        }
    }

    @Test
    fun `should offer schemes for all domains`() {
        DEFAULT_DOMAINS.forEach { (domain, _) ->
            // then: endpoint requires management access token
            assertManagerOnlyGetEndpoint("/api/settings/schema/$domain")

            // given: a permitted token
            val (permittedName, permittedSecret) = useDefaultManagementToken()

            // when: domain schema is requested
            val response = Unirest.get("$base/api/settings/schema/$domain")
                .basicAuth(permittedName, permittedSecret)
                .asString()

            // then: response contains list of all
            assertSuccessResponse(OK, response) {
                val schema = ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.readTree(it)
                assertThat(schema.has("title")).isTrue
                assertThat(schema.has("description")).isTrue
            }
        }
    }

    @Test
    fun `should return settings entity for a domain`() {
        DEFAULT_DOMAINS.forEach { (domain, type) ->
            // then: endpoint requires management access token
            assertManagerOnlyGetEndpoint("/api/settings/domain/$domain")

            // given: a permitted token
            val (permittedName, permittedSecret) = useDefaultManagementToken()

            // when: settings entity is requested
            val response = Unirest.get("$base/api/settings/domain/$domain")
                .basicAuth(permittedName, permittedSecret)
                .asJacksonObject(type)

            assertSuccessResponse(OK, response)
        }
    }

    private val customSettings = mapOf(
        "web" to WebSettings(forwardedIp = "test"),
        "authentication" to AuthenticationSettings(ldap = LdapSettings(enabled = true)),
        "statistics" to StatisticsSettings(resolvedRequestsInterval = YEARLY),
        "frontend" to FrontendSettings(id = "test"),
        "maven" to MavenSettings(repositories = emptyList())
    )

    @Test
    fun `should update settings`() {
        // given: an existing token without management permission
        val (unauthorizedToken, unauthorizedSecret) = useAuth("unauthorized-token", "secret")

        // when: configuration is requested without valid access token
        val unauthorizedResponse = Unirest.put("$base/api/settings/domain/")
            .basicAuth(unauthorizedToken, unauthorizedSecret)
            .asJacksonObject(ErrorResponse::class)

        // then: request is rejected
        assertErrorResponse(FORBIDDEN, unauthorizedResponse)

        // given: a permitted token
        val (permittedName, permittedSecret) = useDefaultManagementToken()

        customSettings.forEach { (domain, configuration) ->
            // when: configuration is updated with valid token
            val updateResponse = Unirest.put("$base/api/settings/domain/$domain")
                .basicAuth(permittedName, permittedSecret)
                .body(configuration)
                .asEmpty()

            // then: configuration update request succeeded
            assertSuccessResponse(OK, updateResponse)

            // when: settings entity is requested
            val response = Unirest.get("$base/api/settings/domain/$domain")
                .basicAuth(permittedName, permittedSecret)
                .asJacksonObject(configuration::class)

            // then: configuration updated succeeded
            assertThat(response.body).isEqualTo(configuration)
        }
    }

    @Test
    fun `should reset configuration to default values`() {
        // given: a permitted token and a facade with custom configuration
        val (permittedName, permittedSecret) = useDefaultManagementToken()

        val settingsFacade = useFacade<SharedConfigurationFacade>()
        customSettings.forEach { (domain, configuration) -> settingsFacade.updateSharedSettings(domain, configuration) }

        customSettings.forEach { (domain, configuration) ->
            // when: empty configuration is set
            val updateResponse = Unirest.put("$base/api/settings/domain/$domain")
                .basicAuth(permittedName, permittedSecret)
                .body("{}")
                .asEmpty()

            // then: update request succeeded
            assertSuccessResponse(OK, updateResponse)

            // when: settings updated settings entity is requested
            val response = Unirest.get("$base/api/settings/domain/$domain")
                .basicAuth(permittedName, permittedSecret)
                .asJacksonObject(configuration::class)

            // then: response is equal to the default configuration instance
            assertThat(response.body).isEqualTo(configuration.javaClass.getConstructor().newInstance())
        }
    }

}