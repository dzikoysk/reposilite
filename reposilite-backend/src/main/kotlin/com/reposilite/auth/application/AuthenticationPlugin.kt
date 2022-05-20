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

package com.reposilite.auth.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.auth.infrastructure.AuthenticationEndpoint
import com.reposilite.auth.infrastructure.PostAuthHandler
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import com.reposilite.web.api.RoutingSetupEvent
import panda.std.reactive.Reference.Dependencies.dependencies
import panda.std.reactive.Reference.computed

@Plugin(name = "authentication", dependencies = ["failure", "shared-configuration", "access-token"], settings = AuthenticationSettings::class)
internal class AuthenticationPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        val failureFacade = facade<FailureFacade>()
        val accessTokenFacade = facade<AccessTokenFacade>()
        val sharedConfigurationFacade = facade<SharedConfigurationFacade>()
        val authenticationSettings = sharedConfigurationFacade.getDomainSettings<AuthenticationSettings>()

        val authenticationFacade = AuthenticationFacade(
            journalist = this,
            authenticators = listOf(
                BasicAuthenticator(accessTokenFacade),
                LdapAuthenticator(
                    ldapSettings = computed(dependencies(authenticationSettings)) { authenticationSettings.map { it.ldap } },
                    accessTokenFacade = accessTokenFacade,
                    failureFacade = failureFacade
                )
            ),
            accessTokenFacade = accessTokenFacade
        )

        event { event: RoutingSetupEvent ->
            event.registerRoutes(AuthenticationEndpoint(authenticationFacade))
            event.registerRoutes(PostAuthHandler())
        }

        return authenticationFacade
    }

}
