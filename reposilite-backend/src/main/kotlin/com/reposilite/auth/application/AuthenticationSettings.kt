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
import com.reposilite.configuration.shared.api.Min
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
    val oauth2: OAuth2Settings = OAuth2Settings(),
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

@Doc(title = "OAuth2", description = "OAuth2 / OpenID Connect settings")
data class OAuth2Settings(
    @get:Doc(title = "Enabled", description = "OAuth2 authentication is enabled")
    val enabled: Boolean = false,
    @get:Doc(title = "Provider Name", description = "Name of the identity provider displayed on the sign in button")
    val providerName: String = "SSO",
    @get:Doc(title = "Issuer", description = "Issuer URL of the OpenID Connect provider (the one that serves /.well-known/openid-configuration)")
    val issuer: String = "https://sso.domain.com/realms/company",
    @get:Doc(title = "Client ID", description = "Identifier of the client registered in the identity provider")
    val clientId: String = "reposilite",
    @get:Doc(title = "Client Secret", description = "Secret of the client registered in the identity provider (leave empty for public clients)")
    val clientSecret: String = "",
    @get:Doc(title = "Scopes", description = "Space separated list of scopes requested during sign in")
    val scopes: String = "openid profile email",
    @get:Doc(title = "Username Claim", description = "Token claim that represents unique username used to create access token")
    val usernameClaim: String = "preferred_username",
    @get:Doc(title = "User Type", description = "Should the created through OAuth2 access token be TEMPORARY or PERSISTENT")
    val userType: AccessTokenType = PERSISTENT,
    @get:Doc(title = "Redirect Base URL", description = "Public URL of this Reposilite instance used to build the redirect URI (leave empty to detect it from the request)")
    val redirectBaseUrl: String = "",
    @Min(1)
    @get:Doc(title = "Session Duration Minutes", description = "Duration in minutes of the dashboard session created after sign in")
    val sessionDurationMinutes: Long = 480,
    @get:Doc(title = "Bearer Tokens", description = "Accept JWTs issued by the identity provider as credentials (Authorization: Bearer or basic auth password)")
    val bearerEnabled: Boolean = false,
    @get:Doc(title = "Bearer Audience", description = "Audience (aud claim) required in JWTs used as credentials")
    val bearerAudience: String = ""
) : SharedSettings
