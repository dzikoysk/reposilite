/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.infrastructure.DatabaseConnection
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePostInitializeEvent
import com.reposilite.plugin.api.ReposiliteStartedEvent
import com.reposilite.shared.extensions.shutdownGracefully
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.web.HttpServer
import org.jetbrains.exposed.v1.jdbc.Database
import panda.std.Result
import panda.std.Result.ok
import panda.std.asError
import panda.std.peek
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.atomic.AtomicBoolean

class Reposilite(
    val journalist: ReposiliteJournalist,
    val parameters: ReposiliteParameters,
    val localConfiguration: LocalConfiguration,
    val databaseConnection: DatabaseConnection,
    val ioService: ExecutorService,
    val scheduler: ScheduledExecutorService,
    val webServer: HttpServer,
    val extensions: Extensions
) : Facade, Journalist {

    val database: Database = databaseConnection.database
    val remoteClientProvider: RemoteClientProvider = HttpRemoteClientProvider(journalist)

    private val alive = AtomicBoolean(true)

    private val shutdownHook = Thread {
        alive.peek { shutdown() }
    }

    fun launch(): Result<Reposilite, Exception> =
        try {
            extensions.emitEvent(ReposiliteInitializeEvent(this))
            extensions.emitEvent(ReposilitePostInitializeEvent(this))
            alive.set(true)
            Thread.currentThread().name = "Reposilite | Main Thread"
            logger.info("")
            logger.info("Binding server at ${parameters.hostname}::${parameters.port}")
            webServer.start(this)
            Runtime.getRuntime().addShutdownHook(shutdownHook)
            extensions.emitEvent(ReposiliteStartedEvent(this))
            ok(this)
        } catch (exception: Exception) {
            logger.error("Failed to start Reposilite")
            logger.exception(exception)
            shutdown()
            exception.asError()
        }

    fun shutdown() =
        alive.peek {
            alive.set(false)
            logger.info("Shutting down ${parameters.hostname}::${parameters.port}...")
            shutdownSafely("scheduler") { scheduler.shutdown() }
            shutdownSafely("web server") { webServer.stop() }
            shutdownSafely("IO executor") {
                if (!ioService.shutdownGracefully(1, MINUTES)) {
                    logger.warn("IO executor did not terminate")
                }
            }
            shutdownSafely("extensions") { extensions.emitEvent(ReposiliteDisposeEvent(this)) }
            shutdownSafely("database") { databaseConnection.close() }
            shutdownSafely("journalist") { journalist.shutdown() }
        }

    private inline fun shutdownSafely(component: String, action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            logger.error("Failed to shut down $component")
            logger.exception(exception)
        }
    }

    override fun getLogger(): Logger =
        journalist.logger

}
