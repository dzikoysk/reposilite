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

package com.reposilite

import com.reposilite.auth.application.AuthenticationPlugin
import com.reposilite.console.application.ConsolePlugin
import com.reposilite.frontend.application.FrontendWebConfiguration
import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.maven.application.MavenWebConfiguration
import com.reposilite.settings.application.DatabaseSourceFactory
import com.reposilite.settings.application.SettingsWebConfiguration
import com.reposilite.shared.extensions.newFixedThreadPool
import com.reposilite.shared.extensions.newSingleThreadScheduledExecutor
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.statistics.application.StatisticsWebConfiguration
import com.reposilite.status.application.FailureWebConfiguration
import com.reposilite.status.application.StatusWebConfiguration
import com.reposilite.token.application.AccessTokenWebConfiguration
import com.reposilite.web.HttpServer
import panda.utilities.console.Effect

object ReposiliteFactory {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite =
        createReposilite(parameters, PrintStreamLogger(System.out, System.err, Channel.ALL, false))

    fun createReposilite(parameters: ReposiliteParameters, rootJournalist: Journalist): Reposilite {
        val localConfiguration = SettingsWebConfiguration.createLocalConfiguration(parameters)
        parameters.applyLoadedConfiguration(localConfiguration)

        val journalist = ReposiliteJournalist(rootJournalist, localConfiguration.cachedLogSize.get(), parameters.testEnv)
        journalist.logger.info("")
        journalist.logger.info("${Effect.GREEN}Reposilite ${Effect.RESET}$VERSION")
        journalist.logger.info("")
        journalist.logger.info("--- Environment")
        journalist.logger.info("Platform: ${System.getProperty("java.version")} (${System.getProperty("os.name")})")
        journalist.logger.info("Working directory: ${parameters.workingDirectory.toAbsolutePath()}")
        journalist.logger.info("Threads: ${localConfiguration.webThreadPool.get()} WEB / ${localConfiguration.ioThreadPool.get()} IO")
        if (parameters.testEnv) journalist.logger.info("Test environment enabled")
        journalist.logger.info("")
        journalist.logger.info("--- Initializing context")

        val scheduler = newSingleThreadScheduledExecutor("Reposilite | Scheduler")
        val ioService = newFixedThreadPool(2, localConfiguration.ioThreadPool.get(), "Reposilite | IO")
        val database = DatabaseSourceFactory.createConnection(parameters.workingDirectory, localConfiguration.database.get())
        val webServer = HttpServer()

        val domains = mutableListOf<DomainComponent>()
        val settingsFacade = domain(domains, SettingsWebConfiguration) { createFacade(journalist, parameters, localConfiguration, database) }
        val statusFacade = domain(domains, StatusWebConfiguration) { createFacade(parameters.testEnv, webServer) }
        val failureFacade = domain(domains, FailureWebConfiguration) { createFacade(journalist) }
        val consoleFacade = domain(domains, ConsolePlugin) { createFacade(journalist, failureFacade) }
        val statisticFacade = domain(domains, StatisticsWebConfiguration) { createFacade(journalist, database, settingsFacade) }
        val frontendFacade = domain(domains, FrontendWebConfiguration) { createFacade(localConfiguration, settingsFacade) }
        val accessTokenFacade = domain(domains, AccessTokenWebConfiguration) { createFacade(database) }
        val authenticationFacade = domain(domains, AuthenticationPlugin) { createFacade(journalist, accessTokenFacade) }
        val mavenFacade = domain(domains, MavenWebConfiguration) {
            createFacade(
                journalist,
                parameters.workingDirectory,
                HttpRemoteClientProvider,
                settingsFacade.sharedConfiguration.repositories,
                statisticFacade
            )
        }
        val badgeFacade = domain(domains, BadgeWebConfiguration) { createFacade(settingsFacade, mavenFacade) }

        return Reposilite(
            journalist = journalist,
            parameters = parameters,
            ioService = ioService,
            scheduler = scheduler,
            database = database,
            webServer = webServer
        )
    }

    private fun <COMPONENT : DomainComponent, FACADE> domain(domains: MutableCollection<DomainComponent>, domain: COMPONENT, block: COMPONENT.() -> FACADE): FACADE {
        domains.add(domain)
        return block(domain)
    }

}