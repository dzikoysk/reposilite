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

package com.reposilite.plugin.javadoc

import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.javadoc.infrastructure.JavadocEndpoints
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "javadoc", dependencies = ["maven"])
internal class JavadocPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val javadocFolder = extensions().parameters.workingDirectory.resolve("javadocs")
        val mavenFacade = facade<MavenFacade>()
        val facade = JavadocFacade(javadocFolder, mavenFacade, this)

        event { event: RoutingSetupEvent ->
            event.registerRoutes(JavadocEndpoints(facade))
        }

        return facade
    }

}