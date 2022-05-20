/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.status.application

import com.reposilite.Reposilite
import com.reposilite.console.ConsoleFacade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.api.ReposiliteStartedEvent
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.shared.extensions.TimeUtils
import com.reposilite.status.FailureFacade
import com.reposilite.status.FailuresCommand
import com.reposilite.status.StatusCommand
import com.reposilite.status.StatusFacade
import com.reposilite.status.infrastructure.RouteAccessHandler
import com.reposilite.web.HttpServer
import com.reposilite.web.api.HttpServerStoppedEvent
import com.reposilite.web.api.RoutingSetupEvent
import panda.std.reactive.Completable

@Plugin(name = "status", dependencies = ["console", "failure"])
internal class StatusPlugin : ReposilitePlugin() {

    private val REMOTE_VERSION = "https://maven.reposilite.com/api/maven/latest/version/releases/org/panda-lang/reposilite?type=raw"

    override fun initialize(): StatusFacade {
        val reposilite = facade<Reposilite>()
        val consoleFacade = facade<ConsoleFacade>()
        val failureFacade = facade<FailureFacade>()
        val webServer = Completable<HttpServer>()

        val statusFacade = StatusFacade(
            testEnv = reposilite.parameters.testEnv,
            status = { if (webServer.isReady) webServer.get().isAlive() else false },
            remoteVersionUrl = REMOTE_VERSION
        )

        event { _: ReposiliteInitializeEvent ->
            webServer.complete(reposilite.webServer)
            consoleFacade.registerCommand(FailuresCommand(failureFacade))
            consoleFacade.registerCommand(StatusCommand(statusFacade, failureFacade))
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(RouteAccessHandler())
        }

        event { _: ReposiliteStartedEvent ->
            logger.info("Done (${TimeUtils.getPrettyUptimeInSeconds(statusFacade.startTime)})!")
            logger.info("")
        }

        event { _: HttpServerStoppedEvent ->
            logger.info("Bye! Uptime: " + TimeUtils.getPrettyUptimeInMinutes(statusFacade.startTime))
        }

        return statusFacade
    }

}