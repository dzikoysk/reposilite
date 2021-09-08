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
import com.reposilite.web.ReposiliteContextFactory
import com.reposilite.web.application.WebConfiguration
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.asCoroutineDispatcher
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.util.concurrent.Executors

internal object ReposiliteFactory {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite {
        val logger = PrintStreamLogger(System.out, System.err, Channel.ALL, false)

        val configurationLoader = ConfigurationLoader(logger)
        val configuration = configurationLoader.tryLoad(parameters.configurationFile)
        parameters.applyLoadedConfiguration(configuration)

        return createReposilite(parameters, logger, configuration)
    }

    private fun createReposilite(parameters: ReposiliteParameters, journalist: Journalist, configuration: Configuration): Reposilite {
        val logger = ReposiliteJournalist(journalist, configuration.cachedLogSize)

        Log.getProperties().setProperty("org.eclipse.jetty.util.log.announce", "false")
        val threadPool = QueuedThreadPool(configuration.coreThreadPool, 2)
        val dispatcher = DispatcherWithShutdown(threadPool.asCoroutineDispatcher())

        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val database = DatabaseSourceConfiguration.createConnection(parameters.workingDirectory, configuration.database)
        val webServer = WebConfiguration.createWebServer(threadPool)
        val failureFacade = FailureWebConfiguration.createFacade(logger)
        val consoleFacade = ConsoleWebConfiguration.createFacade(logger, dispatcher, failureFacade)
        val mavenFacade = MavenWebConfiguration.createFacade(logger, parameters.workingDirectory, HttpRemoteClient(), configuration.repositories)
        val frontendFacade = FrontendWebConfiguration.createFacade(configuration)
        val statisticFacade = StatisticsWebConfiguration.createFacade(logger, dispatcher)
        val accessTokenFacade = AccessTokenWebConfiguration.createFacade(dispatcher)
        val authenticationFacade = AuthenticationWebConfiguration.createFacade(logger, accessTokenFacade)
        val contextFactory = ReposiliteContextFactory(logger, dispatcher, configuration.forwardedIp, authenticationFacade)

        return Reposilite(
            journalist = logger,
            parameters = parameters,
            configuration = configuration,
            threadPool= threadPool,
            dispatcher = dispatcher,
            scheduler = scheduler,
            database = database,
            webServer = webServer,
            failureFacade = failureFacade,
            contextFactory = contextFactory,
            authenticationFacade = authenticationFacade,
            mavenFacade = mavenFacade,
            consoleFacade = consoleFacade,
            accessTokenFacade = accessTokenFacade,
            frontendFacade = frontendFacade,
            statisticsFacade = statisticFacade
        )
    }

}