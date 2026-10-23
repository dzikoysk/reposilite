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
import com.reposilite.auth.infrastructure.OidcEndpoints
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
        val oidcService = authenticationComponents.oidcService()

        facade<FrontendFacade>().registerPlaceholder(
            key = "{{REPOSILITE.OIDC_ENABLED}}",
            value = authenticationSettings.computed { it.oidc.enabled.toString() },
        )

        event { event: RoutingSetupEvent ->
            event.registerRoutes(AuthenticationEndpoint(authenticationFacade, oidcService))
            event.registerRoutes(OidcEndpoints(oidcService))
            event.registerRoutes(PostAuthHandler())
        }

        return authenticationFacade
    }

}
