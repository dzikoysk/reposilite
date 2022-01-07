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

package com.reposilite.maven.application

import com.reposilite.frontend.FrontendFacade
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
import com.reposilite.settings.SettingsFacade
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.web.api.HttpServerInitializationEvent
import com.reposilite.web.api.RoutingSetupEvent
import io.javalin.http.Handler
import io.javalin.http.Stage

@Plugin(name = "maven", dependencies = ["settings", "statistics", "frontend"])
internal class MavenPlugin : ReposilitePlugin() {

    override fun initialize(): MavenFacade {
        val settingsFacade = facade<SettingsFacade>()
        val sharedConfiguration = settingsFacade.sharedConfiguration

        val statisticsFacade = facade<StatisticsFacade>()
        val frontendFacade = facade<FrontendFacade>()

        val repositoryProvider = RepositoryProvider(
            this,
            extensions().parameters.workingDirectory,
            HttpRemoteClientProvider,
            sharedConfiguration.repositories
        )
        val securityProvider = RepositorySecurityProvider()
        val repositoryService = RepositoryService(this, repositoryProvider, securityProvider)

        val mavenFacade = MavenFacade(
            this,
            sharedConfiguration.id,
            securityProvider,
            repositoryService,
            ProxyService(this),
            MetadataService(repositoryService),
            extensions(),
            statisticsFacade,
        )

        logger.info("")
        logger.info("--- Repositories")
        mavenFacade.getRepositories().forEach { logger.info("+ ${it.name} (${it.visibility.toString().lowercase()})") }
        logger.info("${mavenFacade.getRepositories().size} repositories have been found")

        event { event: RoutingSetupEvent ->
            event.registerRoutes(MavenEndpoints(mavenFacade, frontendFacade, settingsFacade))
            event.registerRoutes(MavenApiEndpoints(mavenFacade))
            event.registerRoutes(MavenLatestApiEndpoints(mavenFacade, settingsFacade))
        }

        event(PreservedBuildsListener(mavenFacade))

        event { _: ReposiliteDisposeEvent ->
            mavenFacade.getRepositories().forEach {
                it.shutdown()
            }
        }

        val afterHandlers = listOf<Handler>() // idk, custom handlers defined through plugin API

        event { event: HttpServerInitializationEvent ->
            event.javalin.javalinServlet().addLifecycleStages(Stage("after-without-errors", tasksInitialization = { submitTask ->
                submitTask {
                    afterHandlers.forEach { handler ->
                        handler.handle(this.ctx)
                    }
                }
            }))
        }

        return mavenFacade
    }
}