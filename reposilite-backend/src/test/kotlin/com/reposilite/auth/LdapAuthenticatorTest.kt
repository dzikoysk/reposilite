package com.reposilite.auth

import com.reposilite.assertCollectionsEquals
import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.net.InetAddress

class LdapAuthenticatorTest {

    private lateinit var ldapConfiguration: LdapConfiguration
    private lateinit var ldapServer: InMemoryDirectoryServer

    @BeforeEach
    fun createLdapServer() {
        this.ldapConfiguration = LdapConfiguration().also {
            it.hostname = "ldap.domain.com"
            it.port = (1024 + Math.random() * (Short.MAX_VALUE - 1025)).toInt()
            it.baseDn = "dc=domain,dc=com"
            it.searchUserDn = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com"
            it.searchUserPassword = "search-secret"
        }

        val config = InMemoryDirectoryServerConfig(ldapConfiguration.baseDn)
        config.addAdditionalBindCredentials(ldapConfiguration.searchUserDn, ldapConfiguration.searchUserPassword)
        config.addAdditionalBindCredentials("cn=dzikoysk,ou=Reposilite Users,${ldapConfiguration.baseDn}", "secret")
        config.listenerConfigs.add(InMemoryListenerConfig.createLDAPConfig(ldapConfiguration.hostname, InetAddress.getLoopbackAddress(), ldapConfiguration.port, null))
        config.schema = null // remove

        this.ldapServer = InMemoryDirectoryServer(config)
        ldapServer.startListening(ldapConfiguration.hostname)
        ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")
        ldapServer.add("dn: ou=Search Accounts,dc=domain,dc=com", "objectClass: organizationalUnit")
        ldapServer.add("dn: cn=Reposilite,ou=Search Accounts,dc=domain,dc=com", "objectClass: person", "memberOf: ou=Search Accounts,dc=domain,dc=com")

        ldapConfiguration.hostname = ldapServer.getListenAddress(ldapConfiguration.hostname).hostAddress
    }

    @AfterEach
    fun shutdownLdapServer() {
        ldapServer.shutDown(true)
    }

    @Test
    fun `should connect with search user`() {
        val authenticator = LdapAuthenticator(ldapConfiguration)
        val result = assertOk(authenticator.search("(&(objectClass=person)(cn=Reposilite))", "cn"))

        assertCollectionsEquals(
            listOf(
                mapOf("cn" to listOf(("Reposilite")))
            ),
            result
        )
    }

}