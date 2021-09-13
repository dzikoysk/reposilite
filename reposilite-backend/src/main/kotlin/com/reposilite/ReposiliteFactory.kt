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
import com.reposilite.config.Configuration
import com.reposilite.config.ConfigurationLoader
import com.reposilite.config.DatabaseSourceConfiguration
import com.reposilite.console.application.ConsoleWebConfiguration
import com.reposilite.failure.application.FailureWebConfiguration
import com.reposilite.frontend.application.FrontendWebConfiguration
import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.maven.application.MavenWebConfiguration
import com.reposilite.shared.HttpRemoteClient
import com.reposilite.statistics.application.StatisticsWebConfiguration
import com.reposilite.token.application.AccessTokenWebConfiguration
import com.reposilite.web.JavalinWebServer
import com.reposilite.web.coroutines.ExclusiveDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS

internal object ReposiliteFactory {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite {
        val logger = PrintStreamLogger(System.out, System.err, Channel.ALL, false)

        val configurationLoader = ConfigurationLoader(logger)
        val configuration = configurationLoader.tryLoad(parameters.configurationFile)
        parameters.applyLoadedConfiguration(configuration)

        return createReposilite(parameters, logger, configuration)
    }

    private fun createReposilite(parameters: ReposiliteParameters, journalist: Journalist, configuration: Configuration): Reposilite {
        val webServer = JavalinWebServer()
        val logger = ReposiliteJournalist(journalist, configuration.cachedLogSize, parameters.testEnv)
        val ioDispatcher = ExclusiveDispatcher(ThreadPoolExecutor(2, configuration.ioThreadPool, 0L, MILLISECONDS, LinkedBlockingQueue()))
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val database = DatabaseSourceConfiguration.createConnection(parameters.workingDirectory, configuration.database)
        val failureFacade = FailureWebConfiguration.createFacade(logger)
        val consoleFacade = ConsoleWebConfiguration.createFacade(logger, ioDispatcher, failureFacade)
        val mavenFacade = MavenWebConfiguration.createFacade(logger, parameters.workingDirectory, HttpRemoteClient(), configuration.repositories)
        val frontendFacade = FrontendWebConfiguration.createFacade(configuration)
        val statisticFacade = StatisticsWebConfiguration.createFacade(logger, ioDispatcher, database)
        val accessTokenFacade = AccessTokenWebConfiguration.createFacade(ioDispatcher, database)
        val authenticationFacade = AuthenticationWebConfiguration.createFacade(logger, accessTokenFacade)

        return Reposilite(
            journalist = logger,
            parameters = parameters,
            configuration = configuration,
            ioDispatcher = ioDispatcher,
            scheduler = scheduler,
            database = database,
            webServer = webServer,
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