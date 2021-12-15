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

package com.reposilite.status.application

import com.reposilite.Reposilite
import com.reposilite.status.StatusCommand
import com.reposilite.status.StatusFacade
import com.reposilite.status.infrastructure.RouteAccessHandler
import com.reposilite.web.JavalinWebServer
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes

object StatusWebConfiguration : WebConfiguration {

    private const val REMOTE_VERSION = "https://repo.panda-lang.org/api/maven/latest/version/releases/org/panda-lang/reposilite?type=raw"

    fun createFacade(testEnv: Boolean, webServer: JavalinWebServer): StatusFacade {
        return StatusFacade(testEnv, status = { webServer.isAlive() }, remoteVersionUrl = REMOTE_VERSION)
    }

    override fun initialize(reposilite: Reposilite) {
        reposilite.consoleFacade.registerCommand(StatusCommand(reposilite.statusFacade, reposilite.failureFacade))
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        RouteAccessHandler()
    )

}