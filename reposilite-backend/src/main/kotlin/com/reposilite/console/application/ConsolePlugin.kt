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
package com.reposilite.console.application

import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.console.infrastructure.ConsoleSseHandler
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.reposilite
import com.reposilite.web.api.HttpServerInitializationEvent
import com.reposilite.web.application.WebSettings
import io.javalin.http.sse.SseClient

@Plugin(name = "console", dependencies = [ "shared-configuration", "failure", "access-token", "authentication" ])
internal class ConsolePlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        val sharedConfigurationFacade = facade<SharedConfigurationFacade>()
        val client = ConsoleSseHandler(
            journalist = reposilite().journalist,
            accessTokenFacade = facade(),
            authenticationFacade = facade(),
            forwardedIp = sharedConfigurationFacade.getDomainSettings<WebSettings>().computed { it.forwardedIp },
            scheduler = reposilite().scheduler
        )

        event { event: HttpServerInitializationEvent ->
            event.config.router.mount {
                it.sse(
                    "/api/console/log",
                    client::handleSseLiveLog
                )
            }
        }

        event { _: ReposiliteDisposeEvent ->
            client.users.keys.forEach(SseClient::close)
        }

        return null
    }

}
