package com.reposilite.auth.application

import com.reposilite.settings.api.Doc
import java.io.Serializable

@Doc(title = "LDAP", description = "LDAP Authenticator settings")
data class LdapSettings(
    @Doc(title = "Enabled", description = "LDAP Authenticator is enabled")
    val enabled: Boolean = false,
    @Doc(title = "Hostname", description = "LDAP server address")
    val hostname: String = "ldap.domain.com",
    @Doc(title = "Port", description = "LDAP server port")
    val port: Int = 389,
    @Doc(title = "Base DN", description = "Base DN with users")
    val baseDn: String = "dc=company,dc=com",
    @Doc(title = "Search-User DN", description = "User used to perform searches in LDAP server (requires permissions to read all LDAP entries)")
    val searchUserDn: String = "cn=reposilite,ou=admins,dc=domain,dc=com",
    @Doc(title = "Search-User Password", description = "Search user's password")
    val searchUserPassword: String = "reposilite-admin-secret",
    @Doc(title = "User Attribute", description = "Attribute in LDAP that represents unique username used to create access token")
    val userAttribute: String = "cn",
    @Doc(title = "User Filter", description = "LDAP user filter")
    val userFilter: String = "(&(objectClass=person)(ou=Maven Users))",
    @Doc(title = "User Type", description = "Should the created through LDAP access token be TEMPORARY or PERSISTENT")
    val userType: UserType = UserType.PERSISTENT
) : Serializable {
    enum class UserType {
        PERSISTENT,
        TEMPORARY
    }
}
