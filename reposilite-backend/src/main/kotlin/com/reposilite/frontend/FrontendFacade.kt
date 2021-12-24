/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.plugin.api.Facade
import org.intellij.lang.annotations.Language
import panda.std.reactive.Reference
import panda.std.reactive.computed

class FrontendFacade internal constructor(
    private val cacheContent: Reference<Boolean>,
    private val basePath: Reference<String>,
    private val id: Reference<String>,
    private val title: Reference<String>,
    private val description: Reference<String>,
    private val organizationWebsite: Reference<String>,
    private val organizationLogo: Reference<String>,
    private val icpLicense: Reference<String>,
) : Facade {

    private val resources = HashMap<String, String>(0)

    init {
        computed(cacheContent, basePath, id, title, description, organizationWebsite, organizationLogo, icpLicense) {
            resources.clear()
        }
    }

    fun resolve(uri: String, source: () -> String?): String? =
        resources[uri] ?: source()
            ?.let { resolvePlaceholders(it) }
            ?.also { if (cacheContent.get()) resources[uri] = it }

    private fun resolvePlaceholders(source: String): String =
        source
            .replace("{{REPOSILITE.BASE_PATH}}", basePath.get())
            .replace("{{REPOSILITE.VITE_BASE_PATH}}", basePath.get().takeUnless { it == "" || it == "/" }?.replace(Regex("^/|/$"), "") ?: ".")
            .replace("{{REPOSILITE.ID}}", id.get())
            .replace("{{REPOSILITE.TITLE}}", title.get())
            .replace("{{REPOSILITE.DESCRIPTION}}", description.get())
            .replace("{{REPOSILITE.ORGANIZATION_WEBSITE}}", organizationWebsite.get())
            .replace("{{REPOSILITE.ORGANIZATION_LOGO}}", organizationLogo.get())
            .replace("{{REPOSILITE.ICP_LICENSE}}", icpLicense.get())

    fun createNotFoundPage(uri: String, details: String): String {
        val dashboardURI = basePath.get() + (if (basePath.get().endsWith("/")) "" else "/") + "#" + uri

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