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

import com.reposilite.auth.application.AuthenticationWebConfiguration
import com.reposilite.console.application.ConsoleWebConfiguration
import com.reposilite.frontend.application.FrontendWebConfiguration
import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.maven.application.MavenWebConfiguration
import com.reposilite.settings.DatabaseSourceFactory
import com.reposilite.settings.application.SettingsWebConfiguration
import com.reposilite.shared.HttpRemoteClient
import com.reposilite.shared.newFixedThreadPool
import com.reposilite.shared.newSingleThreadScheduledExecutor
import com.reposilite.statistics.application.StatisticsWebConfiguration
import com.reposilite.status.application.FailureWebConfiguration
import com.reposilite.status.application.StatusWebConfiguration
import com.reposilite.token.application.AccessTokenWebConfiguration
import com.reposilite.web.JavalinWebServer
import com.reposilite.web.WebConfiguration
import com.reposilite.web.web

object ReposiliteFactory {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite =
        createReposilite(parameters, PrintStreamLogger(System.out, System.err, Channel.ALL, false))

    fun createReposilite(parameters: ReposiliteParameters, journalist: Journalist): Reposilite {
        val localConfiguration = SettingsWebConfiguration.createLocalConfiguration(journalist, parameters)

        parameters.applyLoadedConfiguration(localConfiguration)
        val logger = ReposiliteJournalist(journalist, localConfiguration.cachedLogSize.get(), parameters.testEnv)

        val scheduler = newSingleThreadScheduledExecutor("Reposilite | Scheduler")
        val ioService = newFixedThreadPool(2, localConfiguration.ioThreadPool.get(), "Reposilite | IO")
        val database = DatabaseSourceFactory.createConnection(parameters.workingDirectory, localConfiguration.database.get())

        val webServer = JavalinWebServer()
        val webs = mutableListOf<WebConfiguration>()

        val configFacade = web(webs, SettingsWebConfiguration) { createFacade(journalist, parameters) }
        val sharedConfiguration = configFacade.loadSharedConfiguration()

        val statusFacade = web(webs, StatusWebConfiguration) { createFacade(parameters.testEnv, webServer) }
        val failureFacade = web(webs, FailureWebConfiguration) { createFacade(logger) }
        val consoleFacade = web(webs, ConsoleWebConfiguration) { createFacade(logger, failureFacade) }
        val mavenFacade = web(webs, MavenWebConfiguration) { createFacade(logger, parameters.workingDirectory, HttpRemoteClient(logger), sharedConfiguration.repositories) }
        val frontendFacade = web(webs, FrontendWebConfiguration) { createFacade(localConfiguration, sharedConfiguration) }
        val statisticFacade = web(webs, StatisticsWebConfiguration) { createFacade(logger, database) }
        val accessTokenFacade = web(webs, AccessTokenWebConfiguration) { createFacade(database) }
        val authenticationFacade = web(webs, AuthenticationWebConfiguration) { createFacade(logger, accessTokenFacade) }

        return Reposilite(
            journalist = logger,
            parameters = parameters,
            localConfiguration = localConfiguration,
            sharedConfiguration = sharedConfiguration,
            ioService = ioService,
            scheduler = scheduler,
            database = database,
            webServer = webServer,
            webs = webs,
            statusFacade = statusFacade,
            failureFacade = failureFacade,
            authenticationFacade = authenticationFacade,
            mavenFacade = mavenFacade,
            consoleFacade = consoleFacade,
            accessTokenFacade = accessTokenFacade,
            frontendFacade = frontendFacade,
            statisticsFacade = statisticFacade
        )
    }

}