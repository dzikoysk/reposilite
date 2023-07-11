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

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.token.AccessTokenType
import com.reposilite.token.AccessTokenType.PERSISTENT
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Authentication", description = "Authenticator settings")
data class AuthenticationSettings(
    val ldap: LdapSettings = LdapSettings()
) : SharedSettings

@Doc(title = "LDAP", description = "LDAP Authenticator settings")
data class LdapSettings(
    @get:Doc(title = "Enabled", description = "LDAP Authenticator is enabled")
    val enabled: Boolean = false,
    @get:Doc(title = "SSL", description = "Use SSL (LDAPS) to connect to LDAP server")
    val ssl: Boolean = false,
    @get:Doc(title = "Hostname", description = "LDAP server address")
    val hostname: String = "ldap.domain.com",
    @get:Doc(title = "Port", description = "LDAP server port")
    val port: Int = 389,
    @get:Doc(title = "Base DN", description = "Base DN with users")
    val baseDn: String = "dc=company,dc=com",
    @get:Doc(title = "Search-User DN", description = "User used to perform searches in LDAP server (requires permissions to read all LDAP entries)")
    val searchUserDn: String = "cn=reposilite,ou=admins,dc=domain,dc=com",
    @get:Doc(title = "Search-User Password", description = "Search user's password")
    val searchUserPassword: String = "reposilite-admin-secret",
    @get:Doc(title = "User Attribute", description = "Attribute in LDAP that represents unique username used to create access token")
    val userAttribute: String = "cn",
    @get:Doc(title = "User Filter", description = "LDAP user filter")
    val userFilter: String = "(&(objectClass=person)(ou=Maven Users))",
    @get:Doc(title = "User Type", description = "Should the created through LDAP access token be TEMPORARY or PERSISTENT")
    val userType: AccessTokenType = PERSISTENT
) : SharedSettings
