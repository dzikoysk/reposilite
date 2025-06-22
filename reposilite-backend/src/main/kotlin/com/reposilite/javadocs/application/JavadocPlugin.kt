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

package com.reposilite.javadocs.application

import com.reposilite.javadocs.JavadocContainerService
import com.reposilite.javadocs.JavadocFacade
import com.reposilite.javadocs.infrastructure.JavadocEndpoints
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

@Plugin(name = "javadoc", dependencies = ["failure", "maven"])
internal class JavadocPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val javadocFolder = parameters().workingDirectory.resolve("javadocs")
        val failureFacade = facade<FailureFacade>()
        val mavenFacade = facade<MavenFacade>()

        val javadocContainerService = JavadocContainerService(
            mavenFacade = mavenFacade,
            javadocFolder = javadocFolder,
            failureFacade = failureFacade,
        )

        val javadocFacade = JavadocFacade(
            journalist = this,
            javadocFolder = javadocFolder,
            mavenFacade = mavenFacade,
            javadocContainerService = javadocContainerService
        )

        event { event: DeployEvent ->
            val gav = event.gav
                .takeIf { it.toString().endsWith("-javadoc.jar") }
                ?: return@event

            val artifactRootContainer = javadocContainerService.createContainer(javadocFolder, event.repository, gav.getParent())
            val artifactRootDirectory = artifactRootContainer.javadocContainerPath.toFile().parentFile

            if (artifactRootDirectory.exists()) {
                artifactRootDirectory.deleteRecursively()
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(JavadocEndpoints(javadocFacade))
        }

        return javadocFacade
    }

}