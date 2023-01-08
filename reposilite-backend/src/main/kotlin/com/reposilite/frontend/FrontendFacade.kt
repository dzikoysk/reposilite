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

import com.reposilite.frontend.BasePathFormatter.formatAsViteBasePath
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.plugin.api.Facade
import panda.std.reactive.Reference
import panda.std.reactive.computed
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FrontendFacade internal constructor(
    basePath: Reference<String>,
    private val frontendSettings: Reference<FrontendSettings>
) : Facade {

    private val resources = HashMap<String, ResourceSupplier>(0)
    private val formattedBasePath = basePath.computed { BasePathFormatter.formatBasePath(it) }

    init {
        computed(basePath, formattedBasePath, frontendSettings) {
            resources.clear()
        }
    }

    fun resolve(uri: String, source: Source): ResourceSupplier? =
        resources[uri] ?: createProcessedResource(uri, source)

    private fun createProcessedResource(uri: String, source: Source): ResourceSupplier? =
        source.get()
            ?.let { createLazyPlaceholderResolver().createProcessedResource(it) }
            ?.also { resources[uri] = it }

    private fun createLazyPlaceholderResolver(): LazyPlaceholderResolver =
        with (frontendSettings.get()) {
            LazyPlaceholderResolver(mapOf(
                "{{REPOSILITE.BASE_PATH}}" to formattedBasePath.get(),
                URLEncoder.encode("{{REPOSILITE.BASE_PATH}}", StandardCharsets.UTF_8) to formattedBasePath.get(),

                "{{REPOSILITE.VITE_BASE_PATH}}" to formatAsViteBasePath(formattedBasePath.get()),
                URLEncoder.encode("{{REPOSILITE.VITE_BASE_PATH}}", StandardCharsets.UTF_8) to formatAsViteBasePath(formattedBasePath.get()),

                "{{REPOSILITE.ID}}" to id,
                "{{REPOSILITE.TITLE}}" to title,
                "{{REPOSILITE.DESCRIPTION}}" to description,
                "{{REPOSILITE.ORGANIZATION_WEBSITE}}" to organizationWebsite,
                "{{REPOSILITE.ORGANIZATION_LOGO}}" to organizationLogo,
                "{{REPOSILITE.ICP_LICENSE}}" to icpLicense,
            ))
        }

    fun createNotFoundPage(originUri: String, details: String): String =
        NotFoundTemplate.createNotFoundPage(formattedBasePath.get(), originUri, details)

}
