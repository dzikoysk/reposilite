/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.plugin.swagger

import io.javalin.openapi.plugin.swagger.SwaggerPlugin as SwaggerPluginForJavalin
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.web.api.HttpServerConfigurationEvent

@Plugin(name = "swagger", dependencies = ["shared-configuration", "frontend"])
class SwaggerPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        val frontendSettings = facade<SharedConfigurationFacade>().getDomainSettings<FrontendSettings>()

        event { event: HttpServerConfigurationEvent ->
            event.config.registerPlugin(SwaggerPluginForJavalin { swaggerConfiguration ->
                swaggerConfiguration.title = frontendSettings.map { it.title }
            })
        }

        return null
    }

}