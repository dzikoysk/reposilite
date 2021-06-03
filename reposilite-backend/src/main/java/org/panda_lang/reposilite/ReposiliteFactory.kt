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

package org.panda_lang.reposilite

import net.dzikoysk.dynamiclogger.backend.AggregatedLogger
import net.dzikoysk.dynamiclogger.slf4j.Slf4jLogger
import org.jetbrains.exposed.sql.Database
import org.panda_lang.reposilite.auth.application.AuthenticationWebConfiguration
import org.panda_lang.reposilite.config.ConfigurationLoader
import org.panda_lang.reposilite.console.application.ConsoleWebConfiguration
import org.panda_lang.reposilite.failure.application.FailureWebConfiguration
import org.panda_lang.reposilite.maven.application.MavenWebConfiguration
import org.panda_lang.reposilite.resource.application.ResourceWebConfiguration
import org.panda_lang.reposilite.stats.application.StatsWebConfiguration
import org.panda_lang.reposilite.token.application.AccessTokenWebConfiguration
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.slf4j.LoggerFactory
import java.nio.file.Path

object ReposiliteFactory {

    fun createReposilite(configurationFile: Path, workingDirectory: Path, testEnv: Boolean): Reposilite {
        val logger = AggregatedLogger(
            Slf4jLogger(LoggerFactory.getLogger(Reposilite::class.java))
        )

        val configurationLoader = ConfigurationLoader(logger)
        val configuration = configurationLoader.tryLoad(configurationFile)

        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver") // TOFIX: SQL schemas requires connection at startup, somehow delegate it later

        val failureFacade = FailureWebConfiguration.createFacade(logger)
        val consoleFacade = ConsoleWebConfiguration.createFacade(logger, failureFacade)
        val mavenFacade = MavenWebConfiguration.createFacade(logger, failureFacade, configuration.repositories)
        val resourceFacade = ResourceWebConfiguration.createFacade(configuration)
        val statisticFacade = StatsWebConfiguration.createFacade(logger)
        val accessTokenFacade = AccessTokenWebConfiguration.createFacade(logger)
        val authenticationFacade = AuthenticationWebConfiguration.createFacade(logger, accessTokenFacade, mavenFacade)
        val contextFactory = ReposiliteContextFactory(logger, configuration.forwardedIp, authenticationFacade)

        val reposilite = Reposilite(
            logger = logger,
            configuration = configuration,
            workingDirectory = workingDirectory,
            testEnv = testEnv,
            failureFacade = failureFacade,
            contextFactory = contextFactory,
            authenticationFacade = authenticationFacade,
            mavenFacade = mavenFacade,
            consoleFacade = consoleFacade
        )

        AuthenticationWebConfiguration.initialize()
        FailureWebConfiguration.initialize(consoleFacade, failureFacade)
        ConsoleWebConfiguration.initialize(consoleFacade, reposilite)

        return reposilite
    }

}