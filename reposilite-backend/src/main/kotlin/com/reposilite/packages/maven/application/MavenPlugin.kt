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

package com.reposilite.packages.maven.application

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.packages.maven.MavenFacade
import com.reposilite.packages.maven.PreservedBuildsListener
import com.reposilite.packages.maven.infrastructure.MavenApiEndpoints
import com.reposilite.packages.maven.infrastructure.MavenEndpoints
import com.reposilite.packages.maven.infrastructure.MavenLatestApiEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.web.api.RoutingSetupEvent
import panda.std.reactive.Reference
import java.time.Clock

@Plugin(
    name = "maven",
    dependencies = ["failure", "local-configuration", "shared-configuration", "statistics", "authentication", "access-token", "storage"],
    settings = MavenSettings::class
)
internal class MavenPlugin : ReposilitePlugin() {

    override fun initialize(): MavenFacade {
        val sharedConfigurationFacade = facade<SharedConfigurationFacade>()

        val mavenFacade =
            MavenComponents(
                clock = Clock.systemDefaultZone(),
                workingDirectory = parameters().workingDirectory,
                journalist = this,
                extensions = extensions(),
                remoteClientProvider = HttpRemoteClientProvider,
                failureFacade = facade(),
                storageFacade = facade(),
                authenticationFacade = facade(),
                accessTokenFacade = facade(),
                statisticsFacade = facade(),
                mavenSettings = sharedConfigurationFacade.getDomainSettings<MavenSettings>(),
                id = Reference.reference("repository-id"), // todo: get repo id from cfg
            ).mavenFacade()

        logger.info("")
        logger.info("--- Repositories")
        mavenFacade.getRepositories().forEach { logger.info("+ ${it.name} (${it.visibility.toString().lowercase()})") }
        logger.info("${mavenFacade.getRepositories().size} repositories have been found")

        event { event: RoutingSetupEvent ->
            val localConfiguration = facade<LocalConfiguration>()
            event.registerRoutes(MavenApiEndpoints(mavenFacade))
            event.registerRoutes(MavenEndpoints(mavenFacade, basePath = "/", localConfiguration.compressionStrategy.get())) // todo: check base-path
            event.registerRoutes(MavenLatestApiEndpoints(mavenFacade, localConfiguration.compressionStrategy.get()))
        }

        event(PreservedBuildsListener(mavenFacade))

        event { _: ReposiliteDisposeEvent ->
            mavenFacade.getRepositories().forEach {
                it.shutdown()
            }
        }

        return mavenFacade
    }

}