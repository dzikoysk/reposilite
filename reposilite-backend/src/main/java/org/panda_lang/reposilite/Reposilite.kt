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

import net.dzikoysk.dynamiclogger.Channel
import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import net.dzikoysk.dynamiclogger.backend.AggregatedLogger
import org.panda_lang.reposilite.auth.AuthenticationFacade
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.shared.CachedLogger
import org.panda_lang.reposilite.shared.utils.TimeUtils
import org.panda_lang.reposilite.web.HttpServerConfiguration
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.panda_lang.utilities.commons.console.Effect
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class Reposilite(
    logger: Logger,
    val configuration: Configuration,
    val workingDirectory: Path,
    val testEnv: Boolean,
    val contextFactory: ReposiliteContextFactory,
    val failureFacade: FailureFacade,
    val authenticationFacade: AuthenticationFacade,
    val mavenFacade: MavenFacade,
    val consoleFacade: ConsoleFacade
) : Journalist {

    internal val httpServer = HttpServerConfiguration(this, false)

    val cachedLogger = CachedLogger(Channel.ALL, configuration.cachedLogSize)
    private val logger = AggregatedLogger(logger, cachedLogger)

    private val alive = AtomicBoolean(false)
    private val shutdownHook = Thread { shutdown() }
    private val uptime = System.currentTimeMillis()

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

        logger.info("")
        logger.info("--- Loading domain configurations")
        // Arrays.stream(configurations()).forEach { domainConfigurer -> domainConfigurer.configure(this) }
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

    fun getUptime(): Long =
        System.currentTimeMillis() - uptime

    fun getPrettyUptime(): String =
        TimeUtils.format(TimeUtils.getUptime(uptime) / 60) + "min"

    override fun getLogger(): Logger =
        logger

}