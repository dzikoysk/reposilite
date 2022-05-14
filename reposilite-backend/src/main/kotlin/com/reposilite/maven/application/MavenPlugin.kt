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

package com.reposilite.maven.application

import com.reposilite.Reposilite
import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MetadataService
import com.reposilite.maven.PreservedBuildsListener
import com.reposilite.maven.ProxyService
import com.reposilite.maven.RepositoryProvider
import com.reposilite.maven.RepositorySecurityProvider
import com.reposilite.maven.RepositoryService
import com.reposilite.maven.infrastructure.MavenApiEndpoints
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.maven.infrastructure.MavenLatestApiEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.local.LocalConfiguration
import com.reposilite.settings.shared.SharedConfigurationFacade
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "maven", dependencies = ["failure", "local-configuration", "shared-configuration", "statistics", "frontend", "access-token", "storage"])
internal class MavenPlugin : ReposilitePlugin() {

    override fun initialize(): MavenFacade {
        val reposilite = facade<Reposilite>()
        val failureFacade = facade<FailureFacade>()
        val statisticsFacade = facade<StatisticsFacade>()
        val accessTokenFacade = facade<AccessTokenFacade>()
        val storageFacade = facade<StorageFacade>()
        val localConfiguration = facade<LocalConfiguration>()
        val sharedConfigurationFacade = facade<SharedConfigurationFacade>()
        val mavenSettings = sharedConfigurationFacade.createDomainSettings(MavenSettings())

        val frontendFacade = facade<FrontendFacade>()
        val frontendSettings = sharedConfigurationFacade.getDomainSettings<FrontendSettings>()

        val repositoryProvider = RepositoryProvider(
            workingDirectory = reposilite.parameters.workingDirectory,
            remoteClientProvider = HttpRemoteClientProvider,
            failureFacade = failureFacade,
            storageFacade = storageFacade,
            repositoriesSource = mavenSettings.computed { it.repositories }
        )
        val securityProvider = RepositorySecurityProvider(accessTokenFacade)
        val repositoryService = RepositoryService(this, repositoryProvider, securityProvider)

        val mavenFacade = MavenFacade(
            journalist = this,
            repositoryId = frontendSettings.computed { it.id },
            repositorySecurityProvider = securityProvider,
            repositoryService = repositoryService,
            proxyService = ProxyService(this),
            metadataService = MetadataService(repositoryService),
            extensions = extensions(),
            statisticsFacade = statisticsFacade,
        )

        logger.info("")
        logger.info("--- Repositories")
        mavenFacade.getRepositories().forEach { logger.info("+ ${it.name} (${it.visibility.toString().lowercase()})") }
        logger.info("${mavenFacade.getRepositories().size} repositories have been found")

        event { event: RoutingSetupEvent ->
            event.registerRoutes(MavenApiEndpoints(mavenFacade))
            event.registerRoutes(MavenEndpoints(mavenFacade, frontendFacade, localConfiguration.compressionStrategy.get()))
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
