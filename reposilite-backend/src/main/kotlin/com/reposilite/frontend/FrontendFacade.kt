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

import com.reposilite.frontend.application.AppearanceSettings
import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.AdvancedSettings
import org.intellij.lang.annotations.Language
import panda.std.reactive.Reference
import panda.std.reactive.computed

class FrontendFacade internal constructor(
    private val cacheContent: Reference<Boolean>,
    private val appearanceSettings: Reference<AppearanceSettings>,
    private val advancedSettings: Reference<AdvancedSettings>,
) : Facade {

    private val resources = HashMap<String, String>(0)
    private val uriFormatter = Regex("/+") // exclude common typos from URI

    init {
        computed(cacheContent, appearanceSettings, advancedSettings) {
            resources.clear()
        }
    }

    fun resolve(uri: String, source: () -> String?): String? =
        resources[uri] ?: source()
            ?.let { resolvePlaceholders(it) }
            ?.also { if (cacheContent.get()) resources[uri] = it }

    private fun resolvePlaceholders(source: String): String =
        source
            .replace("{{REPOSILITE.BASE_PATH}}", advancedSettings.get().basePath)
            .replace("{{REPOSILITE.VITE_BASE_PATH}}", advancedSettings.get().basePath.takeUnless { it == "" || it == "/" }?.replace(Regex("^/|/$"), "") ?: ".")
            .replace("{{REPOSILITE.ID}}", appearanceSettings.get().id)
            .replace("{{REPOSILITE.TITLE}}", appearanceSettings.get().title)
            .replace("{{REPOSILITE.DESCRIPTION}}", appearanceSettings.get().description)
            .replace("{{REPOSILITE.ORGANIZATION_WEBSITE}}", appearanceSettings.get().organizationWebsite)
            .replace("{{REPOSILITE.ORGANIZATION_LOGO}}", appearanceSettings.get().organizationLogo)
            .replace("{{REPOSILITE.ICP_LICENSE}}", advancedSettings.get().icpLicense)

    fun createNotFoundPage(originUri: String, details: String): String {
        val uri = originUri.replace(uriFormatter, "/")
        val dashboardURI = advancedSettings.get().basePath + (if (advancedSettings.get().basePath.endsWith("/")) "" else "/") + "#" + uri

        @Language("html")
        val response = """
        <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <title>Reposilite - 404 Not Found</title>
            </head>
            <style>
              body {
                height: calc(100vh - 170px);
                display: flex;
                justify-content: center;
                align-items: center;
                font-family: Arial, Helvetica, sans-serif;
              }
            
              .error-view {
                text-align: center;
                width: 100vh;
                height: 100px;
              }
            
              .spooky p {
                margin-top: 0;
                margin-bottom: 0;
                font-size: 1.2rem;
                font-weight: lighter;
              }
              
              a:link, a:visited {
                color: rebeccapurple;
              }
            </style>
            <body>
              <div class='error-view'>
                <h1 style="font-size: 1.5rem">
                  <span style="color: gray;">404︱</span>Resource not found
                </h1>
                ${if (details.isEmpty()) "" else "<p><i>$details</i></p>" }
                <p>Looking for a dashboard?</p>
                <div class="spooky">
                  <p>{\__/}</p>
                  <p>(●ᴗ●)</p>
                  <p>( >🥕</p>
                </div>
                <p>Visit <a href="$dashboardURI" style="color: rebeccapurple; text-decoration: none;">$dashboardURI</a></p>
              </div>
            </body>
        </html>
        """

        return response.trimIndent()
    }

}
