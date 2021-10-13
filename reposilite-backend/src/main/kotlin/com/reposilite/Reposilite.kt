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

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.config.Configuration
import com.reposilite.console.ConsoleFacade
import com.reposilite.frontend.FrontendFacade
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.shared.TimeUtils.getPrettyUptimeInSeconds
import com.reposilite.shared.peek
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.JavalinWebServer
import com.reposilite.web.WebConfiguration
import org.jetbrains.exposed.sql.Database
import panda.utilities.console.Effect
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

const val VERSION = "3.0.0-alpha.7"

class Reposilite(
    val journalist: ReposiliteJournalist,
    val parameters: ReposiliteParameters,
    val configuration: Configuration,
    val ioService: ExecutorService,
    val scheduler: ScheduledExecutorService,
    val database: Database,
    val webServer: JavalinWebServer,
    val webs: Collection<WebConfiguration>,
    val statusFacade: StatusFacade,
    val failureFacade: FailureFacade,
    val authenticationFacade: AuthenticationFacade,
    val mavenFacade: MavenFacade,
    val consoleFacade: ConsoleFacade,
    val accessTokenFacade: AccessTokenFacade,
    val frontendFacade: FrontendFacade,
    val statisticsFacade: StatisticsFacade,
) : Journalist {

    private val alive = AtomicBoolean(false)

    private val shutdownHook = Thread {
        alive.peek { shutdown() }
    }

    fun launch() {
        load()
        start()
    }

    fun load() {
        logger.info("")
        logger.info("${Effect.GREEN}Reposilite ${Effect.RESET}$VERSION")
        logger.info("")
        logger.info("--- Environment")

        if (parameters.testEnv) {
            logger.info("Test environment enabled")
        }

        logger.info("Platform: ${System.getProperty("java.version")} (${System.getProperty("os.name")})")
        logger.info("Working directory: ${parameters.workingDirectory.toAbsolutePath()}")
        logger.info("Threads: ${configuration.webThreadPool} WEB / ${configuration.ioThreadPool} IO")
        logger.info("")

        logger.info("--- Loading domain configurations")
        webs.forEach { it.initialize(this) }
        logger.info("Loaded ${webs.size} web configurations")
        logger.info("")

        logger.info("--- Repositories")
        mavenFacade.getRepositories().forEach { logger.info("+ ${it.name} (${it.visibility.toString().lowercase()})") }
        logger.info("${mavenFacade.getRepositories().size} repositories have been found")
        logger.info("")
    }

    private fun start(): Reposilite {
        alive.set(true)
        Thread.currentThread().name = "Reposilite | Main Thread"

        try {

            logger.info("Binding server at ${parameters.hostname}::${parameters.port}")
            webServer.start(this)
            Runtime.getRuntime().addShutdownHook(shutdownHook)

            logger.info("Done (${getPrettyUptimeInSeconds(statusFacade.startTime)})!")
            logger.info("")
            consoleFacade.executeCommand("help")

            ioService.execute {
                logger.info("")
                logger.info("Collecting status metrics...")
                logger.info("")
                consoleFacade.executeCommand("status")
                logger.info("")
            }
        } catch (exception: Exception) {
            logger.error("Failed to start Reposilite")
            logger.exception(exception)
            shutdown()
            return this
        }

        return this
    }

    fun shutdown() =
        alive.peek {
            alive.set(false)
            logger.info("Shutting down ${parameters.hostname}::${parameters.port}...")
            scheduler.shutdown()
            ioService.shutdown()
            webs.forEach { it.dispose(this@Reposilite) }
            webServer.stop()
            scheduler.shutdownNow()
            ioService.shutdownNow()
            journalist.shutdown()
        }

    override fun getLogger(): Logger =
        journalist.logger

}