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

package com.reposilite.auth

import com.reposilite.assertCollectionsEquals
import com.reposilite.auth.api.Credentials
import com.reposilite.auth.specification.AuthenticationSpecification
import com.reposilite.auth.application.LdapSettings
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import java.net.InetAddress

internal class LdapAuthenticatorTest : AuthenticationSpecification() {

    private lateinit var ldapServer: InMemoryDirectoryServer
    private lateinit var authenticator: LdapAuthenticator

    @BeforeEach
    fun createLdapServer() {
        this.ldapConfiguration.update {
            LdapSettings(
                enabled = it.enabled,
                hostname = "ldap.domain.com",
                port = (1024 + Math.random() * (Short.MAX_VALUE - 1025)).toInt(),
                baseDn = "dc=domain,dc=com",
                searchUserDn = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com",
                searchUserPassword = "search-secret",
                userAttribute = "cn",
                userFilter = "(&(objectClass=person)(ou=Maven Users))",
                userType = it.userType
            )
        }
        this.ldapConfiguration.peek {
            val config = InMemoryDirectoryServerConfig(it.baseDn)
            config.addAdditionalBindCredentials(it.searchUserDn, it.searchUserPassword)
            config.addAdditionalBindCredentials("cn=Bella Swan,ou=Maven Users,dc=domain,dc=com", "secret")
            config.addAdditionalBindCredentials("cn=James Smith,ou=Maven Users,dc=domain,dc=com", "secret2")
            config.listenerConfigs.add(InMemoryListenerConfig.createLDAPConfig(it.hostname, InetAddress.getLoopbackAddress(), it.port, null))
            config.schema = null // remove
            this.ldapServer = InMemoryDirectoryServer(config)
            ldapServer.startListening(it.hostname)
            this.ldapConfiguration.update(
                it.copy(
                    hostname = ldapServer.getListenAddress(it.hostname).hostAddress
                )
            )

            ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")

            ldapServer.add("dn: ou=Search Accounts,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add("dn: ou=Maven Users,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")

            ldapServer.add("dn: cn=Reposilite,ou=Search Accounts,dc=domain,dc=com", "ou:Search Accounts", "objectClass: person", "memberOf: ou=Search Accounts,dc=domain,dc=com")
            ldapServer.add("dn: cn=Bella Swan,ou=Maven Users,dc=domain,dc=com", "cn:Bella Swan", "ou:Maven Users", "objectClass: person", "memberOf: ou=Maven Users,dc=domain,dc=com")
            ldapServer.add("dn: cn=James Smith,ou=Maven Users,dc=domain,dc=com", "cn:James Smith", "ou:Maven Users", "objectClass: person", "memberOf: ou=Maven Users,dc=domain,dc=com")

        }

        this.authenticator = LdapAuthenticator(ldapConfiguration, accessTokenFacade, failureFacade)
    }

    @AfterEach
    fun shutdownLdapServer() {
        ldapServer.shutDown(true)
    }

    @Test
    fun `should connect with search user`() {
        val result = assertOk(authenticator.search("(&(objectClass=person)(cn=Reposilite)(ou=Search Accounts))", arrayOf(), arrayOf("cn")))
        assertCollectionsEquals(
            result,
            listOf(
                SearchEntry(
                    fullName = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com",
                    attributes = mapOf("cn" to listOf("Reposilite")))
            )
        )
    }

    @Test
    fun `should authenticate non-existing ldap user`() {
        val authenticationResult = authenticator.authenticate(
            Credentials(
                name = "Bella Swan",
                secret = "secret"
            )
        )

        val accessToken = assertOk(authenticationResult)
        assertThat(accessToken.name).isEqualTo("Bella Swan")
        assertThat(accessToken).isEqualTo(accessTokenFacade.getAccessToken("Bella Swan"))
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
            Credentials(
                name = token.name,
                secret = secret
            )
        )

        val accessToken = assertOk(authenticationResult)
        assertThat(accessToken.name).isEqualTo("Bella Swan")
        assertThat(accessToken).isEqualTo(token)
    }

}
