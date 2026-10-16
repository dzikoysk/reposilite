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

package com.reposilite.auth.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.token.AccessTokenType
import com.reposilite.token.AccessTokenType.PERSISTENT
import io.javalin.openapi.JsonSchema

@JsonSchema(requireNonNulls = false)
@Doc(title = "Authentication", description = "Authenticator settings")
data class AuthenticationSettings(
    @get:Doc(title = "Brute Force Protection", description = "Rate limiting for failed authentication attempts")
    val bruteForceProtection: BruteForceProtectionSettings = BruteForceProtectionSettings(),
    val ldap: LdapSettings = LdapSettings(),
    val oidc: OidcSettings = OidcSettings(),
) : SharedSettings

@Doc(title = "Brute Force Protection", description = "Brute force protection settings")
data class BruteForceProtectionSettings(
    @get:Doc(title = "Enabled", description = "Enable brute force protection")
    val enabled: Boolean = false,
    @get:Doc(title = "Max Attempts", description = "Maximum failed authentication attempts before blocking an IP")
    val maxAttempts: Int = 10,
    @get:Doc(title = "Ban Duration Seconds", description = "Duration in seconds an IP is blocked after exceeding max attempts")
    val banDurationSeconds: Long = 300
)

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
    @get:Doc(title = "Type Attribute", description = "Attribute in LDAP that represents given type/group (objectClass)")
    val typeAttribute: String = "person",
    @get:Doc(title = "User Attribute", description = "Attribute in LDAP that represents unique username used to create access token")
    val userAttribute: String = "cn",
    @get:Doc(title = "User Filter", description = "LDAP user filter")
    val userFilter: String = "(&(objectClass=$typeAttribute)(ou=Maven Users))",
    @get:Doc(title = "User Type", description = "Should the created through LDAP access token be TEMPORARY or PERSISTENT")
    val userType: AccessTokenType = PERSISTENT
) : SharedSettings

@Doc(title = "OIDC", description = "OpenID Connect login for the web interface, users then create their own tokens for Maven & Gradle")
data class OidcSettings(
    @get:Doc(title = "Enabled", description = "Sign in button uses OpenID Connect")
    val enabled: Boolean = false,
    @get:Doc(title = "Discovery URL", description = "Issuer base URL, '/.well-known/openid-configuration' is appended")
    val discoveryUrl: String = "https://idp.domain.com/realms/company",
    @get:Doc(title = "Client ID", description = "Client ID of the application registered in the identity provider")
    val clientId: String = "reposilite",
    @get:Doc(title = "Client Secret", description = "Client secret of the application registered in the identity provider")
    val clientSecret: String = "",
    @get:Doc(title = "Redirect URI", description = "Public URL of the callback endpoint, must be registered in the identity provider")
    val redirectUri: String = "https://repo.domain.com/oidc/callback",
    @get:Doc(title = "Username Claim", description = "Userinfo claim used as Reposilite user name")
    val usernameClaim: String = "email",
    @get:Doc(title = "Roles Claim", description = "Userinfo claim with the list of user roles")
    val rolesClaim: String = "roles",
    @get:Doc(title = "User Role", description = "Role required to sign in (manager role is enough as well), empty to allow every user of the identity provider")
    val userRole: String = "reposilite-user",
    @get:Doc(title = "Manager Role", description = "Role that grants the manager permission, empty to manage permissions only in Reposilite")
    val managerRole: String = "reposilite-manager",
    @get:Doc(title = "Token Lifetime Days", description = "Lifetime of tokens created by users for Maven & Gradle")
    val tokenLifetimeDays: Long = 90,
    @get:Doc(title = "Session Hours", description = "How long a web interface login stays valid")
    val sessionHours: Long = 8,
) : SharedSettings
