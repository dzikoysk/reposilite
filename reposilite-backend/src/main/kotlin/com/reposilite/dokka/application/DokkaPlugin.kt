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

package com.reposilite.dokka.application

import com.reposilite.dokka.DokkaContainerService
import com.reposilite.dokka.DokkaFacade
import com.reposilite.dokka.infrastructure.DokkaEndpoints
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.status.FailureFacade
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "dokka", dependencies = ["failure", "maven"])
internal class DokkaPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val dokkaFolder = parameters().workingDirectory.resolve("dokka")
        val failureFacade = facade<FailureFacade>()
        val mavenFacade = facade<MavenFacade>()

        val dokkaContainerService = DokkaContainerService(
            mavenFacade = mavenFacade,
            dokkaFolder = dokkaFolder,
            failureFacade = failureFacade,
        )

        val dokkaFacade = DokkaFacade(
            journalist = this,
            dokkaFolder = dokkaFolder,
            mavenFacade = mavenFacade,
            dokkaContainerService = dokkaContainerService
        )

        event { event: DeployEvent ->
            val gav = event.gav
                .takeIf { it.toString().endsWith("-dokka.jar") }
                ?: return@event

            val container = dokkaContainerService.createContainer(dokkaFolder, event.repository, gav)
            val dokkaDirectory = container.dokkaContainerPath.toFile()

            if (dokkaDirectory.exists()) {
                dokkaDirectory.deleteRecursively()
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(DokkaEndpoints(dokkaFacade))
        }

        return dokkaFacade
    }

}