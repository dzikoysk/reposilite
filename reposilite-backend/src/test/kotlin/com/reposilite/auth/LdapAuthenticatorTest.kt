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

package com.reposilite.auth

import com.reposilite.assertCollectionsEquals
import com.reposilite.auth.api.Credentials
import com.reposilite.auth.application.AuthenticationComponents
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.auth.specification.LdapSpecification
import com.reposilite.shared.extractFromString
import com.reposilite.token.AccessTokenPermission.MANAGER
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.api.CreateAccessTokenRequest
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSimpleBindResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import com.unboundid.ldap.sdk.BindResult
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType.REPLACE
import com.unboundid.ldap.sdk.ResultCode
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
                    ip = "host",
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
            val createdToken = accessTokenFacade.createAccessToken(
                CreateAccessTokenRequest(
                    type = TEMPORARY,
                    name = "Bella Swan",
                    secret = "secret"
                )
            )

            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = createdToken.accessToken.name,
                    secret = createdToken.secret
                )
            )

            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("Bella Swan")
            assertThat(accessToken).isEqualTo(createdToken.accessToken)
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
                    "uid=mykola,ou=people,dc=domain,dc=com" to "mykola-secret",
                    "uid=panda,ou=people,dc=domain,dc=com" to "panda-secret"
                )
            )
        }

        @BeforeEach
        fun setup() {
            ldapServer.add("dn: dc=domain,dc=com", "objectClass: top", "objectClass: domain")
            ldapServer.add("dn: cn=readonly,dc=domain,dc=com", "objectClass: posixAccount")

            ldapServer.add("dn: ou=people,dc=domain,dc=com", "objectClass: organizationalUnit", "objectClass: top")
            ldapServer.add("dn: uid=mykola,ou=people,dc=domain,dc=com", "objectClass: posixAccount")
            ldapServer.add("dn: uid=panda,ou=people,dc=domain,dc=com", "objectClass: posixAccount", "memberOf: cn=users,ou=reposilite,dc=domain,dc=com")
        }

        @Test
        fun `should not authenticate mykola`() {
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "mykola",
                    secret = "mykola-secret"
                )
            )

            assertError(authenticationResult)
        }

        @Test
        fun `should authenticate panda`() {
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "panda",
                    secret = "panda-secret"
                )
            )

            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("panda")
            assertThat(accessToken).isEqualTo(accessTokenFacade.getAccessToken("panda"))
        }
    }

    @Nested
    inner class PasswordAuthentication {

        private val userDn = "cn=Alice Cullen,dc=domain,dc=com"
        private val userBinds = AtomicInteger()

        init {
            ldapConfiguration.update {
                it.copy(
                    baseDn = "dc=domain,dc=com",
                    searchUserDn = "cn=Reposilite,dc=domain,dc=com",
                    searchUserPassword = "search-secret",
                    typeAttribute = "person",
                    userAttribute = "cn",
                    userFilter = ""
                )
            }

            createLdapServer(
                ldapSettings = ldapConfiguration.get(),
                baseDns = listOf(ldapConfiguration.get().baseDn),
                credentials = mapOf(
                    ldapConfiguration.map { it.searchUserDn } to ldapConfiguration.map { it.searchUserPassword }
                ),
                operationInterceptor = object : InMemoryOperationInterceptor() {
                    override fun processSimpleBindResult(result: InMemoryInterceptedSimpleBindResult) {
                        if (result.request.bindDN == userDn) {
                            userBinds.incrementAndGet()
                            if (result.request.password.valueLength == 0) {
                                // Model RFC 4513 unauthenticated success without authenticating the connection.
                                result.setResult(BindResult(result.result.messageID, ResultCode.SUCCESS, null, null, null, null))
                            }
                        }
                    }
                }
            )
        }

        @BeforeEach
        fun setup() {
            ldapServer.add("dn: dc=domain,dc=com", "objectClass: domain", "dc: domain")
            ldapServer.add(
                "dn: $userDn",
                "objectClass: person",
                "cn: Alice Cullen",
                "sn: Cullen",
                "userPassword: user-secret"
            )
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\u0000"])
        fun `should reject invalid password without provisioning a token`(password: String) {
            // given: the directory accepts the supplied password
            ldapServer.modify(userDn, Modification(REPLACE, "userPassword", password))
            val basicCredentials = assertOk(extractFromString("Alice Cullen:$password"))

            // when: authentication is requested with an invalid password
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = basicCredentials.username,
                    secret = basicCredentials.password
                )
            )

            // then: authentication is rejected
            assertThat(assertError(authenticationResult).status).isEqualTo(401)
            assertThat(accessTokenFacade.getAccessToken("Alice Cullen")).isNull()
            assertThat(userBinds.get()).isZero()
        }

        @Test
        fun `should reject empty password for an existing manager token through authentication facade`() {
            // given: an existing LDAP user has a manager token
            accessTokenFacade.createAccessToken(
                CreateAccessTokenRequest(
                    type = TEMPORARY,
                    name = "Alice Cullen",
                    secret = "local-secret",
                    permissions = setOf(MANAGER)
                )
            )

            // when: authentication is requested with an empty password
            val authenticationResult = authenticationFacade.authenticateByCredentials(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = ""
                )
            )

            // then: authentication is rejected
            assertThat(assertError(authenticationResult).status).isEqualTo(401)
            assertThat(userBinds.get()).isZero()
        }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `should reject stored empty secrets left by earlier LDAP provisioning`(ldapEnabled: Boolean) {
            // given: an earlier LDAP login left a token with an empty secret
            val token = createToken("Alice Cullen", "")
            assertThat(accessTokenFacade.secretMatches(token.identifier, "")).isTrue()
            ldapConfiguration.update { it.copy(enabled = ldapEnabled) }

            // when: authentication is requested with an empty password
            val authenticationResult = authenticationFacade.authenticateByCredentials(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = ""
                )
            )

            // then: authentication is rejected
            assertThat(assertError(authenticationResult).status).isEqualTo(401)
        }

        @ParameterizedTest
        @ValueSource(strings = ["user-secret", " user-secret "])
        fun `should accept valid nonblank passwords without trimming`(password: String) {
            // given: the directory accepts the supplied password
            ldapServer.modify(userDn, Modification(REPLACE, "userPassword", password))

            // when: authentication is requested with the exact password
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = password
                )
            )

            // then: the user is authenticated without trimming the password
            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("Alice Cullen")
            assertThat(userBinds.get()).isEqualTo(1)
        }

        @Test
        fun `should reject incorrect nonempty password`() {
            // when: authentication is requested with an incorrect password
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = "wrong-secret"
                )
            )

            // then: authentication fails without creating a token
            assertError(authenticationResult)
            assertThat(accessTokenFacade.getAccessToken("Alice Cullen")).isNull()
        }

        @Test
        fun `should allow anonymous search context while verifying user password`() {
            // given: directory searches use an anonymous connection
            ldapConfiguration.update { it.copy(searchUserDn = "", searchUserPassword = "") }

            // when: the user authenticates with a valid password
            val authenticationResult = authenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = "user-secret"
                )
            )

            // then: anonymous directory searches remain supported
            val accessToken = assertOk(authenticationResult)
            assertThat(accessToken.name).isEqualTo("Alice Cullen")
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "arbitrary-secret"])
        fun `should preserve explicitly enabled passwordless authentication`(password: String) {
            // given: password verification is explicitly disabled
            val passwordlessFacade = AuthenticationComponents(
                journalist = logger,
                accessTokenFacade = accessTokenFacade,
                failureFacade = failureFacade,
                authenticationSettings = ldapConfiguration.computed { AuthenticationSettings(ldap = it) },
                disableUserPasswordAuthentication = true
            ).authenticationFacade()

            // when: a new user authenticates through the facade
            val facadeResult = passwordlessFacade.authenticateByCredentials(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = password
                )
            )

            // then: LDAP accepts the user without a password bind
            val accessToken = assertOk(facadeResult)
            assertThat(accessToken.name).isEqualTo("Alice Cullen")
            assertThat(userBinds.get()).isZero()
        }

        @Test
        fun `should reject excluded users with passwordless authentication`() {
            // given: password verification is disabled but a user filter is configured
            ldapConfiguration.update { it.copy(userFilter = "(ou=restricted)") }
            val passwordlessAuthenticator = LdapAuthenticator(
                journalist = logger,
                ldapSettings = ldapConfiguration,
                accessTokenFacade = accessTokenFacade,
                failureFacade = failureFacade,
                disableUserPasswordAuthentication = true
            )

            // when: a user outside that filter attempts to authenticate
            val authenticationResult = passwordlessAuthenticator.authenticate(
                Credentials(
                    ip = "host",
                    name = "Alice Cullen",
                    secret = ""
                )
            )

            // then: authentication is rejected without creating a token
            assertError(authenticationResult)
            assertThat(accessTokenFacade.getAccessToken("Alice Cullen")).isNull()
            assertThat(userBinds.get()).isZero()
        }
    }

}
