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
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.application.WebConfiguration
import io.javalin.Javalin
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.Executors

internal object ReposiliteWebConfiguration {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite {
        val logger = PrintStreamLogger(System.out, System.err, Channel.ALL, false)

        val configurationLoader = ConfigurationLoader(logger)
        val configuration = configurationLoader.tryLoad(parameters.configurationFile)
        parameters.applyLoadedConfiguration(configuration)

        // TOFIX: SQL schemas requires connection at startup, somehow delegate it later
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;mode=MySQL", driver = "org.h2.Driver")

        return createReposilite(parameters, logger, configuration)
    }

    private fun createReposilite(parameters: ReposiliteParameters, journalist: Journalist, configuration: Configuration): Reposilite {
        val logger = ReposiliteJournalist(journalist, configuration.cachedLogSize)
        val coreThreadPool = QueuedThreadPool(configuration.coreThreadPool, 2)
        val scheduler = Executors.newSingleThreadScheduledExecutor()

        val webServer = WebConfiguration.createWebServer()
        val failureFacade = FailureWebConfiguration.createFacade(logger)
        val consoleFacade = ConsoleWebConfiguration.createFacade(logger, failureFacade)
        val mavenFacade = MavenWebConfiguration.createFacade(logger, parameters.workingDirectory, HttpRemoteClient(), configuration.repositories)
        val frontendFacade = FrontendWebConfiguration.createFacade(configuration)
        val statisticFacade = StatisticsWebConfiguration.createFacade(logger)
        val accessTokenFacade = AccessTokenWebConfiguration.createFacade()
        val authenticationFacade = AuthenticationWebConfiguration.createFacade(logger, accessTokenFacade)
        val contextFactory = ReposiliteContextFactory(logger, configuration.forwardedIp, authenticationFacade)

        return Reposilite(
            journalist = logger,
            parameters = parameters,
            configuration = configuration,
            coreThreadPool = coreThreadPool,
            scheduler = scheduler,
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

    fun initialize(reposilite: Reposilite) {
        // AuthenticationWebConfiguration.initialize()
        FailureWebConfiguration.initialize(reposilite.consoleFacade, reposilite.failureFacade)
        ConsoleWebConfiguration.initialize(reposilite.consoleFacade, reposilite)
        // MavenWebConfiguration.initialize()
        // FrontendWebConfiguration.initialize()
        // MavenWebConfiguration.initialize()
        StatisticsWebConfiguration.initialize(reposilite.statisticsFacade, reposilite.consoleFacade, reposilite.scheduler)
        AccessTokenWebConfiguration.initialize(reposilite.accessTokenFacade, reposilite.parameters.tokens, reposilite.consoleFacade)
    }

    fun routing(reposilite: Reposilite): Array<ReposiliteRoutes> =
        setOf(
            AuthenticationWebConfiguration.routing(reposilite.authenticationFacade),
            ConsoleWebConfiguration.routing(reposilite),
            FailureWebConfiguration.routing(),
            FrontendWebConfiguration.routing(reposilite.frontendFacade, reposilite.parameters.workingDirectory),
            MavenWebConfiguration.routing(reposilite.mavenFacade, reposilite.frontendFacade),
            StatisticsWebConfiguration.routing(reposilite.statisticsFacade),
            AccessTokenWebConfiguration.routing(),
        )
        .flatten()
        .toTypedArray()

    // TOFIX: Remove dependency on infrastructure details
    fun javalin(reposilite: Reposilite, javalin: Javalin) {
        ConsoleWebConfiguration.javalin(javalin, reposilite)
        FailureWebConfiguration.javalin(javalin, reposilite.failureFacade)
        FrontendWebConfiguration.javalin(javalin, reposilite.frontendFacade)
    }

    fun dispose(reposilite: Reposilite) {
        ConsoleWebConfiguration.dispose(reposilite.consoleFacade)
    }

}