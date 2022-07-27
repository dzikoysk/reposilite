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
    private val authenticationSettings: Reference<AuthenticationSettings>
) : PluginComponents {

    private fun basicAuthenticator(): BasicAuthenticator =
        BasicAuthenticator(accessTokenFacade)

    private fun ldapAuthenticator(): LdapAuthenticator =
        LdapAuthenticator(
            ldapSettings = Reference.computed(Dependencies.dependencies(authenticationSettings)) { authenticationSettings.map { it.ldap } },
            accessTokenFacade = accessTokenFacade,
            failureFacade = failureFacade
        )

    private fun authenticators(): List<Authenticator> =
        listOf(
            basicAuthenticator(),
            ldapAuthenticator()
        )

    fun authenticationFacade(authenticators: List<Authenticator> = authenticators()): AuthenticationFacade =
        AuthenticationFacade(
            journalist = journalist,
            authenticators = authenticators,
            accessTokenFacade = accessTokenFacade
        )

}