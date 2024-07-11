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

package com.reposilite.auth.application

import com.reposilite.auth.AuthenticationFacade
import com.reposilite.auth.Authenticator
import com.reposilite.auth.BasicAuthenticator
import com.reposilite.auth.LdapAuthenticator
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.status.FailureFacade
import com.reposilite.token.AccessTokenFacade
import panda.std.reactive.Reference
import panda.std.reactive.Reference.Dependencies

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
            ldapSettings = Reference.computed(Dependencies.dependencies(authenticationSettings)) { authenticationSettings.map { it.ldap } },
            accessTokenFacade = accessTokenFacade,
            failureFacade = failureFacade,
            disableUserPasswordAuthentication = disableUserPasswordAuthentication,
        )

    private fun authenticators(): MutableList<Authenticator> =
        arrayListOf(
            basicAuthenticator(),
            ldapAuthenticator()
        )

    fun authenticationFacade(authenticators: MutableList<Authenticator> = authenticators()): AuthenticationFacade =
        AuthenticationFacade(
            journalist = journalist,
            authenticators = authenticators,
            accessTokenFacade = accessTokenFacade
        )

}
