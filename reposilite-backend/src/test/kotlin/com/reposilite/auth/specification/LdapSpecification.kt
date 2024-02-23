package com.reposilite.auth.specification

import com.reposilite.auth.LdapAuthenticator
import com.reposilite.auth.application.LdapSettings
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import java.net.InetAddress
import org.junit.jupiter.api.AfterEach

internal open class LdapSpecification : AuthenticationSpecification() {

    protected lateinit var ldapServer: InMemoryDirectoryServer
    protected lateinit var authenticator: LdapAuthenticator

    init {
        ldapConfiguration.update {
            it.copy(
                enabled = true,
                hostname = "ldap.domain.com",
                port = (1024 + Math.random() * (Short.MAX_VALUE - 1025)).toInt(),
            )
        }
    }

    fun createLdapServer(ldapSettings: LdapSettings, baseDns: List<String>, credentials: Map<String, String>) {
        val config = InMemoryDirectoryServerConfig(*baseDns.toTypedArray())
        credentials.forEach { (user, password) ->
            config.addAdditionalBindCredentials(user, password)
        }
        config.listenerConfigs.add(InMemoryListenerConfig.createLDAPConfig(ldapSettings.hostname, InetAddress.getLoopbackAddress(), ldapSettings.port, null))
        config.schema = null // remove

        this.ldapServer = InMemoryDirectoryServer(config)
        ldapServer.startListening(ldapSettings.hostname)
        ldapConfiguration.update(ldapSettings.copy(hostname = ldapServer.getListenAddress(ldapSettings.hostname).hostAddress))

        this.authenticator = LdapAuthenticator(
            journalist = logger,
            ldapSettings = ldapConfiguration,
            accessTokenFacade = accessTokenFacade,
            failureFacade = failureFacade
        )
    }

    @AfterEach
    fun shutdownLdapServer() {
        ldapServer.shutDown(true)
    }

}