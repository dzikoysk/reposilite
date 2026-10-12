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

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.Authenticator
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.auth.oauth2.HttpOAuth2ProviderClient
import com.reposilite.auth.oauth2.OAuth2BearerAuthenticator
import com.reposilite.auth.oauth2.OAuth2Service
import com.reposilite.auth.oauth2.OAuth2SessionAuthenticator
import com.reposilite.auth.oauth2.OAuth2SessionStore
import com.reposilite.auth.oauth2.OAuth2TokenVerifier
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import panda.std.reactive.Reference
import java.util.SortedSet

class AuthenticationComponents(
    private val journalist: Journalist,
    private val accessTokenFacade: AccessTokenFacade,
    private val failureFacade: FailureFacade,
    private val authenticationSettings: Reference<AuthenticationSettings>,
    private val disableUserPasswordAuthentication: Boolean = false
) : PluginComponents {

    private fun basicAuthenticator(): BasicAuthenticator =
        BasicAuthenticator(accessTokenFacade)

    private fun ldapAuthenticator(): LdapAuthenticator =
        LdapAuthenticator(
            journalist = journalist,
            accessTokenFacade = accessTokenFacade,
            failureFacade = failureFacade,
            disableUserPasswordAuthentication = disableUserPasswordAuthentication,
            ldapSettings = authenticationSettings.computed { it.ldap },
        )

    internal val oauth2Service: OAuth2Service by lazy {
        val oauth2Settings = authenticationSettings.computed { it.oauth2 }
        val providerClient = HttpOAuth2ProviderClient()

        OAuth2Service(
            journalist = journalist,
            settings = oauth2Settings,
            providerClient = providerClient,
            tokenVerifier = OAuth2TokenVerifier(providerClient, oauth2Settings),
            sessionStore = OAuth2SessionStore(),
            accessTokenFacade = accessTokenFacade
        )
    }

    private fun authenticators(): SortedSet<Authenticator> =
        sortedSetOf(
            Authenticator.priorityComparator,
            OAuth2SessionAuthenticator(oauth2Service),
            basicAuthenticator(),
            OAuth2BearerAuthenticator(oauth2Service),
            ldapAuthenticator()
        )

    fun authenticationFacade(authenticators: SortedSet<Authenticator> = authenticators()): AuthenticationFacade =
        AuthenticationFacade(
            journalist = journalist,
            authenticators = authenticators,
            accessTokenFacade = accessTokenFacade,
            bruteForceProtectionSettings = authenticationSettings.computed { it.bruteForceProtection }
        )

}
