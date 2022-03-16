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

package com.reposilite.frontend.application

import com.reposilite.Reposilite
import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.infrastructure.CustomFrontendHandler
import com.reposilite.frontend.infrastructure.NotFoundHandler
import com.reposilite.frontend.infrastructure.ResourcesFrontendHandler
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.SchemaHandler
import com.reposilite.web.api.HttpServerInitializationEvent
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.api.RoutingSetupEvent
import io.javalin.http.NotFoundResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Plugin(name = "frontend", dependencies = ["settings"])
internal class FrontendPlugin : ReposilitePlugin() {

    internal companion object {
        private const val STATIC_DIRECTORY = "static"
        private const val FRONTEND_DIRECTORY = "reposilite-frontend"
    }

    override fun initialize(): FrontendFacade {
        val settingsFacade = facade<SettingsFacade>()

        settingsFacade.registerSchemaWatcher(
            FrontendSettings::class.java,
            { settingsFacade.sharedConfiguration.appearance.get() },
            { settingsFacade.sharedConfiguration.appearance.update(it) }
        )

        val frontendFacade = FrontendFacade(
            settingsFacade.localConfiguration.cacheContent,
            settingsFacade.sharedConfiguration.appearance
        )

        event { event: ReposiliteInitializeEvent -> staticDirectory(event.reposilite)
            .takeIf { it.exists().not() }
            ?.run {
                Files.createDirectory(this)
                Files.copy(Reposilite::class.java.getResourceAsStream("/$STATIC_DIRECTORY/index.html")!!, resolve("index.html"))
            }
        }

        event { event: RoutingSetupEvent -> event.registerRoutes(
            mutableSetOf<ReposiliteRoutes>().also {
                if (settingsFacade.sharedConfiguration.appearance.map { it.frontend }) {
                    it.add(ResourcesFrontendHandler(frontendFacade, FRONTEND_DIRECTORY))
                }
                it.add(CustomFrontendHandler(frontendFacade, staticDirectory(event.reposilite)))
            }
        )}

        event { event: HttpServerInitializationEvent ->
            event.javalin.exception(NotFoundResponse::class.java, NotFoundHandler(frontendFacade))
            event.javalin.error(404, NotFoundHandler(frontendFacade))
        }

        return frontendFacade
    }

    private fun staticDirectory(reposilite: Reposilite): Path =
        reposilite.parameters.workingDirectory.resolve(STATIC_DIRECTORY)

}
