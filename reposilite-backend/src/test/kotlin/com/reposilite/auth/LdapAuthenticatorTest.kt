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
import com.reposilite.auth.specification.LdapSpecification
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk

internal class LdapAuthenticatorTest : LdapSpecification() {

    @Nested
    inner class DefaultTests {

        init {
            ldapConfiguration.update {
                it.copy(
                    baseDn = "dc=domain,dc=com",
                    searchUserDn = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com",
                    searchUserPassword = "search-secret",
                    userAttribute = "cn",
                    userFilter = "(&(objectClass=person)(ou=Maven Users))",
                    userType = it.userType
                )
            }

            createLdapServer(
                ldapSettings = ldapConfiguration.get(),
                baseDns = listOf(ldapConfiguration.get().baseDn),
                credentials = mapOf(
                    ldapConfiguration.map { it.searchUserDn } to ldapConfiguration.map { it.searchUserPassword },
                    "cn=Bella Swan,ou=Maven Users,dc=domain,dc=com" to "secret",
                    "cn=James Smith,ou=Maven Users,dc=domain,dc=com" to "secret2"
                )
            )
        }

        @BeforeEach
        fun setup() {
            ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")
            ldapServer.add("dn: ou=Search Accounts,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add("dn: ou=Maven Users,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add(
                "dn: cn=Reposilite,ou=Search Accounts,dc=domain,dc=com",
                "ou:Search Accounts",
                "objectClass: person",
                "memberOf: ou=Search Accounts,dc=domain,dc=com"
            )
            ldapServer.add(
                "dn: cn=Bella Swan,ou=Maven Users,dc=domain,dc=com",
                "cn:Bella Swan",
                "ou:Maven Users",
                "objectClass: person",
                "memberOf: ou=Maven Users,dc=domain,dc=com"
            )
            ldapServer.add(
                "dn: cn=James Smith,ou=Maven Users,dc=domain,dc=com",
                "cn:James Smith",
                "ou:Maven Users",
                "objectClass: person",
                "memberOf: ou=Maven Users,dc=domain,dc=com"
            )
        }

        @Test
        fun `should connect with search user`() {
            val result = assertOk(authenticator.search("(&(objectClass=person)(cn=Reposilite)(ou=Search Accounts))", arrayOf(), arrayOf("cn")))
            assertCollectionsEquals(
                result,
                listOf(
                    SearchEntry(
                        fullName = "cn=Reposilite,ou=Search Accounts,dc=domain,dc=com",
                        attributes = mapOf("cn" to listOf("Reposilite"))
                    )
                )
            )
        }

        @Test
        fun `should authenticate non-existing ldap user`() {
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    host = "host",
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
                    host = "host",
                    name = token.name,
                    secret = secret
                )
            )

            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("Bella Swan")
            assertThat(accessToken).isEqualTo(token)
        }

    }

    @Nested
    inner class GH2054 {

        init {
            ldapConfiguration.update {
                it.copy(
                    baseDn = "ou=people,dc=domain,dc=com",
                    searchUserDn = "cn=readonly,dc=domain,dc=com",
                    searchUserPassword = "search-secret",
                    typeAttribute = "posixAccount",
                    userAttribute = "uid",
                    userFilter = "(memberOf=cn=users,ou=reposilite,dc=domain,dc=com)",
                    userType = it.userType
                )
            }

            createLdapServer(
                ldapSettings = ldapConfiguration.get(),
                baseDns = listOf("dc=domain,dc=com", "ou=people,dc=domain,dc=com"),
                credentials = mapOf(
                    "cn=readonly,dc=domain,dc=com" to "search-secret",
                    "uid=mykola,ou=people,dc=domain,dc=com" to "mykola-secret"
                )
            )
        }

        @BeforeEach
        fun setup() {
            ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")

            ldapServer.add("dn: ou=people,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add("dn: ou=reposilite,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")

            ldapServer.add("dn: cn=users,dc=domain,dc=com", "ou: people", "objectClass: posixAccount", "memberOf: ou=people,dc=domain,dc=com")
            ldapServer.add("dn: cn=readonly,dc=domain,dc=com", "ou: reposilite", "uid: readonly", "objectClass: posixAccount", "memberOf: cn=users,ou=reposilite,dc=domain,dc=com")

            ldapServer.add("dn: uid=mykola,ou=people,dc=domain,dc=com", "ou: reposilite", "objectClass: posixAccount", "memberOf: cn=users,ou=reposilite,dc=domain,dc=com")
        }

        @Test
        fun `should authenticate mykola`() {
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    host = "host",
                    name = "mykola",
                    secret = "mykola-secret"
                )
            )

            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("mykola")
            assertThat(accessToken).isEqualTo(accessTokenFacade.getAccessToken("mykola"))
        }

        @Test
        fun `should not authenticate readonly`() {
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    host = "host",
                    name = "readonly",
                    secret = "search-secret"
                )
            )

            assertError(authenticationResult)
        }
    }

}
