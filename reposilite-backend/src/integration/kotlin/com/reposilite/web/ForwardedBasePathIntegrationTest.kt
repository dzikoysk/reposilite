/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.web

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.web.application.WebSettings
import io.javalin.http.HttpStatus.NOT_FOUND
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix"
private const val BASE_PATH = "/maven"

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class ForwardedBasePathIntegrationTest : ReposiliteSpecification() {

    override fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) {
        localConfiguration.basePath.update { BASE_PATH }
    }

    override fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) {
        sharedConfigurationFacade.getDomainSettings<WebSettings>().update { it.copy(forwardedPrefix = FORWARDED_PREFIX_HEADER) }
    }

    @BeforeEach
    fun setupRepository() {
        useDocument("releases", "gav", "artifact.jar", "content", store = true)
    }

    @Test
    fun `should render index links under the configured base path for any forwarded request`() {
        // when: the directory is requested through the proxy with a header value that differs from the base path
        val response = get("$base/releases/gav")
            .header(FORWARDED_PREFIX_HEADER, "/ignored-value")
            .asString()

        // then: the configured base path is used regardless of the header value
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("<base href='/maven/releases/gav/'>")
    }

    @Test
    fun `should render index links at the root when the request is not forwarded`() {
        // when: the directory is requested directly, without the forwarded header
        val response = get("$base/releases/gav").asString()

        // then: links fall back to the root
        assertThat(response.isSuccess).isTrue
        assertThat(response.body).contains("<base href='/releases/gav/'>")
    }

    @Test
    fun `should render the 404 dashboard link under the configured base path when forwarded`() {
        // when: a missing resource is requested through the proxy
        val response = get("$base/releases/unknown-gav/unknown-file")
            .header(FORWARDED_PREFIX_HEADER, "/ignored-value")
            .asString()

        // then: the dashboard link uses the configured base path
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.body).contains("/maven/#")
    }

    @Test
    fun `should render the 404 dashboard link at the root when the request is not forwarded`() {
        // when: a missing resource is requested directly, without the forwarded header
        val response = get("$base/releases/unknown-gav/unknown-file").asString()

        // then: the dashboard link falls back to the root
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.body).contains("href=\"/#")
        assertThat(response.body).doesNotContain("/maven/#")
    }

}

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class DisabledForwardedBasePathIntegrationTest : ReposiliteSpecification() {

    override fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) {
        localConfiguration.basePath.update { BASE_PATH }
    }

    @BeforeEach
    fun setupRepository() {
        useDocument("releases", "gav", "artifact.jar", "content", store = true)
    }

    @Test
    fun `should always use the configured base path when the feature is disabled`() {
        // when: a request arrives without the forwarded header
        val direct = get("$base/releases/gav").asString()

        // then: the base path stays configured, not the root
        assertThat(direct.body).contains("<base href='/maven/releases/gav/'>")

        // when: a request arrives carrying the forwarded header
        val forwarded = get("$base/releases/gav").header(FORWARDED_PREFIX_HEADER, "/other").asString()

        // then: the header is ignored and the base path stays configured
        assertThat(forwarded.body).contains("<base href='/maven/releases/gav/'>")
    }

}
