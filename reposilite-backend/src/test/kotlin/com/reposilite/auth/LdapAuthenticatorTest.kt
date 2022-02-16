package com.reposilite.auth

import com.reposilite.assertCollectionsEquals
import com.reposilite.auth.api.AuthenticationRequest
import com.reposilite.auth.specification.AuthenticationSpecification
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.net.InetAddress

internal class LdapAuthenticatorTest : AuthenticationSpecification() {

    private lateinit var ldapServer: InMemoryDirectoryServer
    private lateinit var authenticator: LdapAuthenticator

    @BeforeEach
    fun createLdapServer() {
        this.ldapConfiguration.peek {
            it.hostname = "ldap.domain.com"
            it.port = (1024 + Math.random() * (Short.MAX_VALUE - 1025)).toInt()
            it.baseDn = "dc=domain,dc=com"
            it.searchUserDn = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com"
            it.searchUserPassword = "search-secret"
            it.userFilter = "(&(objectClass=person)(ou=Maven Users))"
            it.userAttribute = "cn"

            val config = InMemoryDirectoryServerConfig(it.baseDn)
            config.addAdditionalBindCredentials(it.searchUserDn, it.searchUserPassword)
            config.addAdditionalBindCredentials("cn=Bella Swan,ou=Maven Users,dc=domain,dc=com", "secret")
            config.listenerConfigs.add(InMemoryListenerConfig.createLDAPConfig(it.hostname, InetAddress.getLoopbackAddress(), it.port, null))
            config.schema = null // remove

            this.ldapServer = InMemoryDirectoryServer(config)
            ldapServer.startListening(it.hostname)
            it.hostname = ldapServer.getListenAddress(it.hostname).hostAddress

            ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")

            ldapServer.add("dn: ou=Search Accounts,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add("dn: ou=Maven Users,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")

            ldapServer.add("dn: cn=Reposilite,ou=Search Accounts,dc=domain,dc=com", "ou:Search Accounts", "objectClass: person", "memberOf: ou=Search Accounts,dc=domain,dc=com")
            ldapServer.add("dn: cn=Bella Swan,ou=Maven Users,dc=domain,dc=com", "cn:Bella Swan", "ou:Maven Users", "objectClass: person", "memberOf: ou=Maven Users,dc=domain,dc=com")

        }

        this.authenticator = LdapAuthenticator(ldapConfiguration, accessTokenFacade)
    }

    @AfterEach
    fun shutdownLdapServer() {
        ldapServer.shutDown(true)
    }

    @Test
    fun `should connect with search user`() {
        val result = assertOk(authenticator.search("(&(objectClass=person)(cn=Reposilite)(ou=Search Accounts))", "cn"))
        assertCollectionsEquals(listOf(Pair("cn=Reposilite,ou=Search Accounts,dc=domain,dc=com", mapOf("cn" to listOf("Reposilite")))), result)
    }

    @Test
    fun `should authenticate non-existing ldap user`() {
        val authenticationResult = authenticator.authenticate(
            AuthenticationRequest(
                name = "Bella Swan",
                secret = "secret"
            )
        )

        val accessToken = assertOk(authenticationResult)
        assertEquals("Bella Swan", accessToken.name)
        assertEquals(accessTokenFacade.getAccessToken("Bella Swan")!!, accessToken)
    }

    @Test
    fun `should authenticate existing ldap user`() {
        val (token, secret) = accessTokenFacade.createAccessToken(
            CreateAccessTokenRequest(
                type = TEMPORARY,
                name = "Bella Swan",
                secret = "secret"
            )
        )

        val authenticationResult = authenticator.authenticate(
            AuthenticationRequest(
                name = token.name,
                secret = secret
            )
        )

        val accessToken = assertOk(authenticationResult)
        assertEquals("Bella Swan", accessToken.name)
        assertEquals(token, accessToken)
    }

}