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

package com.reposilite.auth.application

import com.reposilite.auth.infrastructure.AuthenticationEndpoint
import com.reposilite.auth.infrastructure.OAuth2Endpoints
import com.reposilite.auth.infrastructure.PostAuthHandler
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.frontend.FrontendFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "authentication", dependencies = ["failure", "shared-configuration", "access-token", "frontend"], settings = AuthenticationSettings::class)
class AuthenticationPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val sharedConfigurationFacade = facade<SharedConfigurationFacade>()
        val frontendFacade = facade<FrontendFacade>()
        val authenticationSettings = sharedConfigurationFacade.getDomainSettings<AuthenticationSettings>()

        val authenticationComponents =
            AuthenticationComponents(
                journalist = this,
                accessTokenFacade = facade(),
                failureFacade = facade(),
                authenticationSettings = authenticationSettings,
                disableUserPasswordAuthentication = System.getProperty("reposilite.ldap.disable-user-password-authentication", "false") == "true"
            )
        val authenticationFacade = authenticationComponents.authenticationFacade()

        frontendFacade.registerPlaceholder(
            key = "{{REPOSILITE.OAUTH2_PROVIDER}}",
            value = authenticationSettings.computed {
                when {
                    it.oauth2.enabled -> escapeForJsString(it.oauth2.providerName.ifBlank { "SSO" })
                    else -> ""
                }
            },
        )

        event { event: RoutingSetupEvent ->
            event.registerRoutes(AuthenticationEndpoint(authenticationFacade))
            event.registerRoutes(
                OAuth2Endpoints(
                    oauth2Service = authenticationComponents.oauth2Service,
                    oauth2Settings = authenticationSettings.computed { it.oauth2 },
                    frontendFacade = frontendFacade
                )
            )
            event.registerRoutes(PostAuthHandler())
        }

        return authenticationFacade
    }

    // The bundler is free to pick any kind of quotes for the string that contains the placeholder
    private fun escapeForJsString(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace(Regex("['\"`$]")) { "\\${it.value}" }
            .replace("\n", " ")

}
