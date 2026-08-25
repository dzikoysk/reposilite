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

package com.reposilite.redirect

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.storage.api.Location
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.RoutingSetupEvent
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.HEAD

@Plugin(name = "redirect", dependencies = ["local-configuration", "frontend", "maven"])
class RedirectPlugin : ReposilitePlugin() {

    private val redirectTo: String? = System.getProperty("reposilite.redirect.default-repository", "")

    override fun initialize(): Facade? {
        if (redirectTo.isNullOrEmpty()) {
            return null
        }

        val mavenFacade = facade<MavenFacade>()

        val mavenEndpoints = MavenEndpoints(
            mavenFacade = mavenFacade,
            frontendFacade = facade<FrontendFacade>(),
            compressionStrategy = facade<LocalConfiguration>().compressionStrategy.get()
        )

        logger.info("")
        logger.info("--- Redirect")

        val redirectedRoutes = mavenFacade.getRepository(redirectTo)
            ?.storageProvider
            ?.getFiles(Location.of("/"))
            ?.orNull()
            ?.map {
                logger.info("Redirecting /${it.getSimpleName()}/<gav> to /$redirectTo/${it.getSimpleName()}/<gav>")

                ReposiliteRoute<Unit>("/${it.getSimpleName()}/<gav>", HEAD, GET) {
                    accessed {
                        mavenEndpoints.findFile(
                            ctx = ctx,
                            identifier = this?.identifier,
                            repository = redirectTo,
                            gav = it.resolve(requireParameter("gav"))
                        )
                    }
                }
            }
            ?: emptyList()

        event { event: RoutingSetupEvent ->
            event.register(redirectedRoutes)
        }

        return null
    }

}