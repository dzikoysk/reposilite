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

package com.reposilite.frontend

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.ReposiliteSpecification
import com.reposilite.configuration.local.LocalConfiguration
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val BASE_PATH = "/custom-base-path"
private const val FONT_SOURCE_PREFIX = "src: url('"

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class FrontendBasePathIntegrationTest : ReposiliteSpecification() {

    override fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) {
        localConfiguration.basePath.update { BASE_PATH }
    }

    @Test
    fun `should resolve bundled frontend assets under the configured base path`() {
        val indexResponse = get("$base/").asString()

        assertThat(indexResponse.isSuccess).isTrue
        assertThat(indexResponse.body)
            .contains("src=\"$BASE_PATH/assets/")
            .contains("href=\"$BASE_PATH/assets/")
            .contains("$FONT_SOURCE_PREFIX$BASE_PATH/assets/")

        val fontPath = indexResponse.body
            .substringAfter(FONT_SOURCE_PREFIX)
            .substringBefore("')")

        assertThat(fontPath)
            .startsWith("$BASE_PATH/assets/")
            .endsWith(".woff2")

        val fontResponse = get("$base${fontPath.removePrefix(BASE_PATH)}").asBytes()

        assertThat(fontResponse.isSuccess).isTrue
        assertThat(fontResponse.headers.getFirst("Content-Type")).isEqualTo("font/woff2")
        assertThat(fontResponse.body).isNotEmpty
    }

}
