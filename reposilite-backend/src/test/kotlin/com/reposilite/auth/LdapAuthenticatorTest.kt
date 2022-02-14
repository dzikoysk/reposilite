package com.reposilite.auth

import com.reposilite.settings.api.SharedConfiguration.LdapConfiguration
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class LdapAuthenticatorTest {

    private lateinit var ldapConfiguration: LdapConfiguration
    private lateinit var ldapServer: InMemoryDirectoryServer

    @BeforeEach
    fun createLdapServer() {
        this.ldapConfiguration = LdapConfiguration().also {
            it.hostname = "ldap.domain.com"
            it.port = (1024 + (Math.random() * (Short.MAX_VALUE - 1025))).toInt()
            it.baseDn = "dc=domain,dc=com"
            it.searchUserDn = "cn=Reposilite,ou=Search Accounts,${it.baseDn}"
            it.searchUserPassword = "search-secret"
        }

        val config = InMemoryDirectoryServerConfig("dc=ldap,${ldapConfiguration.baseDn}")
        config.addAdditionalBindCredentials(ldapConfiguration.searchUserDn, ldapConfiguration.searchUserPassword)
        config.listenerConfigs.add(InMemoryListenerConfig.createLDAPConfig(ldapConfiguration.hostname, InetAddress.getLoopbackAddress(), ldapConfiguration.port, null))

        this.ldapServer = InMemoryDirectoryServer(config)
        ldapServer.startListening(ldapConfiguration.hostname)
        ldapConfiguration.hostname = ldapServer.getListenAddress(ldapConfiguration.hostname).hostAddress
    }

    @AfterEach
    fun shutdownLdapServer() {
        ldapServer.shutDown(true)
    }

    @Test
    fun `should connect with search user`() {
        val authenticator = LdapAuthenticator(ldapConfiguration)
        authenticator.search("(&(objectClass=person)(cn=Reposilite))")
    }

}