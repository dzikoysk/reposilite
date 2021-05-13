/*
 * Copyright (c) 2020 Dzikoysk
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

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.auth.AuthService
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.auth.TokenService
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.console.Console
import org.panda_lang.reposilite.error.FailureService
import org.panda_lang.reposilite.metadata.MetadataService
import org.panda_lang.reposilite.repository.*
import org.panda_lang.reposilite.resource.FrontendProvider
import org.panda_lang.reposilite.stats.StatsService
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.reposilite.utils.TimeUtils
import org.panda_lang.utilities.commons.console.Effect
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Reposilite(
    private val logger: Logger,
    val configuration: Configuration,
    val workingDirectory: Path,
    val testEnv: Boolean,
    val failureService: FailureService,
    val console: Console,
    val contextFactory: ReposiliteContextFactory,
    val statsService: StatsService,
    val storageProvider: StorageProvider,
    val repositoryService: RepositoryService,
    val metadataService: MetadataService,
    val tokenService: TokenService,
    val authenticator: Authenticator,
    val repositoryAuthenticator: RepositoryAuthenticator,
    val authService: AuthService,
    val lookupService: LookupService,
    val proxyService: ProxyService,
    val deployService: DeployService,
    val frontendService: FrontendProvider,
) : Journalist {

    val httpServer = ReposiliteHttpServer(this, false)

    private val alive = AtomicBoolean(false)
    private val shutdownHook = Thread { shutdown() }
    private var uptime = System.currentTimeMillis()

    fun launch() {
        load()
        start()
    }

    fun load() {
        logger.info("")
        logger.info("${Effect.GREEN}Reposilite${Effect.RESET}${ReposiliteConstants.VERSION}")
        logger.info("")
        logger.info("--- Environment")

        if (testEnv) {
            logger.info("Test environment enabled")
        }

        logger.info("Platform: ${System.getProperty("java.version")} (${System.getProperty("os.name")})")
        logger.info("Working directory: ${workingDirectory.toAbsolutePath()}")
        logger.info("")

        logger.info("--- Loading data")
        tokenService.loadTokens()

        logger.info("")
        repositoryService.load(configuration)

        logger.info("")
        logger.info("--- Loading domain configurations")
        Arrays.stream(configurations()).forEach { configurer: ReposiliteConfigurer -> configurer.configure(this) }
    }

    fun start(): Reposilite {
        alive.set(true)
        Thread.currentThread().name = "Reposilite | Main Thread"

        try {
            logger.info("Binding server at ${configuration.hostname}::${configuration.port}")
            httpServer.start(configuration)
            Runtime.getRuntime().addShutdownHook(shutdownHook)
        } catch (exception: Exception) {
            logger.error("Failed to start Reposilite")
            logger.exception(exception)
            shutdown()
            return this
        }

        logger.info("Done (" + TimeUtils.format(TimeUtils.getUptime(uptime)) + "s)!")
        console.defaultExecute("help")

        logger.info("Collecting status metrics...")
        console.defaultExecute("status")

        // disable console daemon in tests due to issues with coverage and interrupt method call
        // https://github.com/jacoco/jacoco/issues/1066
        if (!testEnv) {
            console.hook()
        }

        return this
    }

    @Synchronized
    fun shutdown() {
        if (!alive.get()) {
            return
        }

        alive.set(false)
        Runtime.getRuntime().removeShutdownHook(shutdownHook)

        logger.info("Shutting down ${configuration.hostname}::${configuration.port} ...")
        httpServer.stop()
    }

    fun getPrettyUptime() =
        TimeUtils.format(TimeUtils.getUptime(uptime) / 60) + "min"

    fun getUptime() =
        System.currentTimeMillis() - uptime

    override fun getLogger() =
        logger

}