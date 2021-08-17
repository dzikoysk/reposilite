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
import com.reposilite.console.application.ConsoleWebConfiguration
import com.reposilite.failure.application.FailureWebConfiguration
import com.reposilite.frontend.application.FrontendWebConfiguration
import com.reposilite.maven.application.MavenWebConfiguration
import com.reposilite.shared.HttpRemoteClient
import com.reposilite.statistics.application.StatisticsWebConfiguration
import com.reposilite.token.application.AccessTokenWebConfiguration
import com.reposilite.web.ReposiliteContextFactory
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.application.WebConfiguration
import io.javalin.Javalin
import net.dzikoysk.dynamiclogger.Journalist
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.nio.file.Path

object ReposiliteWebConfiguration {

    fun createReposilite(journalist: Journalist, configuration: Configuration, workingDirectory: Path, testEnv: Boolean): Reposilite {
        val logger = journalist.logger
        val coreThreadPool = QueuedThreadPool(configuration.coreThreadPool, 2)

        val webServer = WebConfiguration.createWebServer()
        val failureFacade = FailureWebConfiguration.createFacade(logger)
        val consoleFacade = ConsoleWebConfiguration.createFacade(logger, failureFacade)
        val mavenFacade = MavenWebConfiguration.createFacade(logger, failureFacade, workingDirectory, HttpRemoteClient(), configuration.repositories)
        val frontendFacade = FrontendWebConfiguration.createFacade()
        val statisticFacade = StatisticsWebConfiguration.createFacade(logger)
        val accessTokenFacade = AccessTokenWebConfiguration.createFacade(logger)
        val authenticationFacade = AuthenticationWebConfiguration.createFacade(logger, accessTokenFacade, mavenFacade)
        val contextFactory = ReposiliteContextFactory(logger, configuration.forwardedIp, authenticationFacade)

        return Reposilite(
            logger = logger,
            configuration = configuration,
            workingDirectory = workingDirectory,
            testEnv = testEnv,
            coreThreadPool = coreThreadPool,
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
        StatisticsWebConfiguration.initialize(reposilite.statisticsFacade, reposilite.consoleFacade)
        AccessTokenWebConfiguration.initialize(reposilite.accessTokenFacade, reposilite.consoleFacade)
    }

    fun routing(reposilite: Reposilite): Array<ReposiliteRoutes> =
        setOf(
            AuthenticationWebConfiguration.routing(reposilite.authenticationFacade),
            ConsoleWebConfiguration.routing(reposilite),
            FailureWebConfiguration.routing(),
            FrontendWebConfiguration.routing(reposilite.frontendFacade, reposilite.workingDirectory),
            MavenWebConfiguration.routing(reposilite.mavenFacade),
            StatisticsWebConfiguration.routing(reposilite.statisticsFacade),
            AccessTokenWebConfiguration.routing(),
        )
        .flatten()
        .toTypedArray()

    // TOFIX: Remove dependency on infrastructure details
    fun javalin(reposilite: Reposilite, javalin: Javalin) {
        ConsoleWebConfiguration.javalin(javalin, reposilite)
        FailureWebConfiguration.javalin(javalin, reposilite.failureFacade)
    }

    fun dispose(reposilite: Reposilite) {
        ConsoleWebConfiguration.dispose(reposilite.consoleFacade)
        StatisticsWebConfiguration.dispose()
    }

}