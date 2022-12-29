/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.plugin.api.Facade
import panda.std.Result
import panda.std.letIf
import panda.std.reactive.Reference
import panda.std.reactive.computed
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class FrontendFacade internal constructor(
    basePath: Reference<String>,
    private val frontendSettings: Reference<FrontendSettings>
) : Facade {

    private val resources = HashMap<String, ResourceSupplier>(0)
    private val formattedBasePath = basePath.computed { formatBasePath(it) }

    init {
        computed(basePath, formattedBasePath, frontendSettings) {
            resources.clear()
        }
    }

    fun resolve(uri: String, source: () -> InputStream?): ResourceSupplier? =
        resources[uri] ?: source()
            ?.let {
                val temporaryResourcePath = Files.createTempFile("reposilite", "frontend-resource")

                it.use { inputStream ->
                    Files.newOutputStream(temporaryResourcePath, StandardOpenOption.WRITE).use { outputStream ->
                        createLazyPlaceholderResolver().process(inputStream, outputStream)
                    }
                }

                ResourceSupplier {
                    Result.supplyThrowing(IOException::class.java) {
                        Files.newInputStream(temporaryResourcePath)
                    }
                }
            }
            ?.also { resources[uri] = it }

    private fun createLazyPlaceholderResolver(): LazyPlaceholderResolver =
        with (frontendSettings.get()) {
            LazyPlaceholderResolver(mapOf(
                "{{REPOSILITE.BASE_PATH}}" to formattedBasePath.get(),
                URLEncoder.encode("{{REPOSILITE.BASE_PATH}}", StandardCharsets.UTF_8) to formattedBasePath.get(),

                "{{REPOSILITE.VITE_BASE_PATH}}" to getViteBasePath(),
                URLEncoder.encode("{{REPOSILITE.VITE_BASE_PATH}}", StandardCharsets.UTF_8) to getViteBasePath(),

                "{{REPOSILITE.ID}}" to id,
                "{{REPOSILITE.TITLE}}" to title,
                "{{REPOSILITE.DESCRIPTION}}" to description,
                "{{REPOSILITE.ORGANIZATION_WEBSITE}}" to organizationWebsite,
                "{{REPOSILITE.ORGANIZATION_LOGO}}" to organizationLogo,
                "{{REPOSILITE.ICP_LICENSE}}" to icpLicense,
            ))
        }

    private fun formatBasePath(originBasePath: String): String =
        originBasePath
            .letIf({ it.isNotEmpty() && !it.startsWith("/") }, { "/$it" })
            .letIf({ it.isNotEmpty() && !it.endsWith("/")}, { "$it/" })

    private val pathRegex = Regex("^/|/$")

    private fun getViteBasePath(): String =
        formattedBasePath.get()
            .takeIf { hasCustomBasePath() }
            ?.replace(pathRegex, "") // remove first & last slash
            ?: "." // no custom base path

    private fun hasCustomBasePath(): Boolean =
        formattedBasePath.map { it != "" && it != "/" }

    private fun String.resolvePathPlaceholder(placeholder: String, value: String): String =
        this
            .replace(placeholder, value)
            .replace(URLEncoder.encode(placeholder, StandardCharsets.UTF_8), URLEncoder.encode(value, StandardCharsets.UTF_8))

    fun createNotFoundPage(originUri: String, details: String): String =
        NotFoundTemplate.createNotFoundPage(formattedBasePath.get(), originUri, details)

}
