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

package com.reposilite.frontend.application

import com.reposilite.Reposilite
import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.infrastructure.CustomFrontendHandler
import com.reposilite.frontend.infrastructure.ResourcesFrontendHandler
import com.reposilite.settings.LocalConfiguration
import com.reposilite.settings.SharedConfiguration
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes
import io.javalin.Javalin
import io.javalin.http.NotFoundResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

internal object FrontendWebConfiguration : WebConfiguration {

    private const val STATIC_DIRECTORY = "static"
    private const val FRONTEND_DIRECTORY = "reposilite-frontend"

    fun createFacade(localConfiguration: LocalConfiguration, sharedConfiguration: SharedConfiguration): FrontendFacade =
        FrontendFacade(
            localConfiguration.cacheContent,
            sharedConfiguration.basePath,
            sharedConfiguration.id,
            sharedConfiguration.title,
            sharedConfiguration.description,
            sharedConfiguration.organizationWebsite,
            sharedConfiguration.organizationLogo,
            sharedConfiguration.icpLicense
        )

    override fun initialize(reposilite: Reposilite) {
        with (staticDirectory(reposilite)) {
            if (exists().not()) {
                Files.createDirectory(this)
                Files.copy(Reposilite::class.java.getResourceAsStream("/$STATIC_DIRECTORY/index.html")!!, resolve("index.html"))
            }
        }
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = mutableSetOf<ReposiliteRoutes>().also {
        if (reposilite.sharedConfiguration.frontend.get()) {
            it.add(ResourcesFrontendHandler(reposilite.frontendFacade, FRONTEND_DIRECTORY))
        }

        it.add(CustomFrontendHandler(reposilite.frontendFacade, staticDirectory(reposilite)))
    }

    override fun javalin(reposilite: Reposilite, javalin: Javalin) {
        javalin.exception(NotFoundResponse::class.java, NotFoundHandler(reposilite.frontendFacade))
        javalin.error(404, NotFoundHandler(reposilite.frontendFacade))
    }

    private fun staticDirectory(reposilite: Reposilite): Path =
        reposilite.parameters.workingDirectory.resolve(STATIC_DIRECTORY)

}