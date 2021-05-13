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

import net.dzikoysk.dynamiclogger.Logger
import net.dzikoysk.dynamiclogger.LoggerHolder
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
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Reposilite(
    private val xlogger: Logger,
    val configuration: Configuration,
    val workingDirectory: Path,
    val testEnv: Boolean,
    val failureService: FailureService,
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
) {

    val httpServer = ReposiliteHttpServer(this, false)
    val console = Console(System.`in`, failureService)

    private val alive = AtomicBoolean(false)
    private val shutdownHook = Thread { shutdown() }
    private var uptime = 0L

    companion object {
        @JvmStatic
        val logger: org.slf4j.Logger = LoggerFactory.getLogger("xxxx")
    }

    fun launch() {
        load()
        start()
    }

    fun load() {
        xlogger.info("")
        xlogger.info("${Effect.GREEN}Reposilite${Effect.RESET}${ReposiliteConstants.VERSION}")
        xlogger.info("")

        xlogger.info("--- Environment")

        if (testEnv) {
            xlogger.info("Test environment enabled")
        }

        xlogger.info("Platform: ${System.getProperty("java.version")} (${System.getProperty("os.name")})")
        xlogger.info("Working directory: ${workingDirectory.toAbsolutePath()}")
        xlogger.info("")

        xlogger.info("--- Loading data")
        tokenService.loadTokens()
        xlogger.info("")
        repositoryService.load(configuration)
        xlogger.info("")

        xlogger.info("--- Loading domain configurations")
        Arrays.stream(configurations()).forEach { configuration: ReposiliteConfiguration -> configuration.configure(this) }
    }

    fun start(): Reposilite {
        alive.set(true)
        Thread.currentThread().name = "Reposilite | Main Thread"

        try {
            xlogger.info("Binding server at ${configuration.hostname}::${configuration.port}")
            uptime = System.currentTimeMillis()

            httpServer.start(configuration)
            Runtime.getRuntime().addShutdownHook(shutdownHook)
        } catch (exception: Exception) {
            xlogger.error("Failed to start Reposilite")
            xlogger.exception(exception)
            shutdown()
            return this
        }

        xlogger.info("Done (" + TimeUtils.format(TimeUtils.getUptime(uptime)) + "s)!")
        console.defaultExecute("help")

        xlogger.info("Collecting status metrics...")
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

        xlogger.info("Shutting down ${configuration.hostname}::${configuration.port} ...")
        httpServer.stop()
    }

    fun getPrettyUptime() =
        TimeUtils.format(TimeUtils.getUptime(uptime) / 60) + "min"

    fun getUptime() =
        System.currentTimeMillis() - uptime

}