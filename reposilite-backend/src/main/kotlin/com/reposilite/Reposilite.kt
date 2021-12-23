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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.ExtensionsManagement
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePostInitializeEvent
import com.reposilite.plugin.api.ReposiliteStartedEvent
import com.reposilite.shared.extensions.peek
import com.reposilite.web.HttpServer
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

const val VERSION = "3.0.0-alpha.14"

class Reposilite(
    val journalist: ReposiliteJournalist,
    val parameters: ReposiliteParameters,
    val ioService: ExecutorService,
    val scheduler: ScheduledExecutorService,
    val database: Database,
    val webServer: HttpServer,
    val extensionsManagement: ExtensionsManagement
) : Journalist {

    private val alive = AtomicBoolean(false)

    private val shutdownHook = Thread {
        alive.peek { shutdown() }
    }

    fun launch() {
        try {
            extensionsManagement.emitEvent(ReposiliteInitializeEvent(this))
            extensionsManagement.emitEvent(ReposilitePostInitializeEvent(this))
            alive.set(true)
            Thread.currentThread().name = "Reposilite | Main Thread"
            logger.info("")
            logger.info("Binding server at ${parameters.hostname}::${parameters.port}")
            webServer.start(this)
            Runtime.getRuntime().addShutdownHook(shutdownHook)
            extensionsManagement.emitEvent(ReposiliteStartedEvent(this))
        } catch (exception: Exception) {
            logger.error("Failed to start Reposilite")
            logger.exception(exception)
            shutdown()
        }
    }

    fun shutdown() =
        alive.peek {
            alive.set(false)
            logger.info("Shutting down ${parameters.hostname}::${parameters.port}...")
            scheduler.shutdown()
            ioService.shutdown()
            extensionsManagement.emitEvent(ReposiliteDisposeEvent(this))
            webServer.stop()
            scheduler.shutdownNow()
            ioService.shutdownNow()
            journalist.shutdown()
        }

    override fun getLogger(): Logger =
        journalist.logger

}