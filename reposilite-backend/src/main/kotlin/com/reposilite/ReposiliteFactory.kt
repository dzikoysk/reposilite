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

import com.reposilite.journalist.Channel
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.ExtensionsManagement
import com.reposilite.settings.application.DatabaseSourceFactory
import com.reposilite.settings.application.LocalConfigurationFactory
import com.reposilite.shared.extensions.newFixedThreadPool
import com.reposilite.shared.extensions.newSingleThreadScheduledExecutor
import com.reposilite.web.HttpServer
import panda.utilities.console.Effect

object ReposiliteFactory {

    fun createReposilite(parameters: ReposiliteParameters): Reposilite =
        createReposilite(parameters, PrintStreamLogger(System.out, System.err, Channel.ALL, false))

    fun createReposilite(parameters: ReposiliteParameters, rootJournalist: Journalist): Reposilite {
        val localConfiguration = LocalConfigurationFactory.createLocalConfiguration(parameters)
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

        val webServer = HttpServer()
        val scheduler = newSingleThreadScheduledExecutor("Reposilite | Scheduler")
        val ioService = newFixedThreadPool(2, localConfiguration.ioThreadPool.get(), "Reposilite | IO")
        val database = DatabaseSourceFactory.createConnection(parameters.workingDirectory, localConfiguration.database.get())
        val extensionsManagement = ExtensionsManagement(journalist, parameters, localConfiguration, database)

        return Reposilite(
            journalist = journalist,
            parameters = parameters,
            ioService = ioService,
            scheduler = scheduler,
            database = database,
            webServer = webServer,
            extensionsManagement = extensionsManagement
        )
    }

}