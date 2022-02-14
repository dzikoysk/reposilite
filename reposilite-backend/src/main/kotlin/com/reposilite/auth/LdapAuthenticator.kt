package com.reposilite.auth

import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.util.Hashtable
import javax.naming.Context.INITIAL_CONTEXT_FACTORY
import javax.naming.Context.PROVIDER_URL
import javax.naming.Context.SECURITY_AUTHENTICATION
import javax.naming.Context.SECURITY_CREDENTIALS
import javax.naming.Context.SECURITY_PRINCIPAL
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext

internal class LdapAuthenticator(
    private val ldapConfiguration: LdapConfiguration
) : Authenticator {

    override fun authenticate(authenticationRequest: AuthenticationRequest): Result<AccessTokenDto, ErrorResponse> {
        TODO("Not yet implemented")
    }

    fun search(query: String) {
        val searchContext = with(ldapConfiguration) {
            createContext(hostname, port, searchUserDn, searchUserPassword)
        }

        searchContext.close()
    }

    private fun createContext(hostname: String, port: Int, user: String, password: String): DirContext = InitialDirContext(
        Hashtable<String, String>().also {
            it[INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
            it[PROVIDER_URL] = "ldap://$hostname:$port"
            it[SECURITY_AUTHENTICATION] = "simple"
            it[SECURITY_PRINCIPAL] = user
            it[SECURITY_CREDENTIALS] = password
        }
    )

    override fun name(): String =
        "LDAP"

}