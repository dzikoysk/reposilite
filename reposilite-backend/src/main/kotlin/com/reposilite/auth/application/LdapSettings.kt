package com.reposilite.auth.application

import java.io.Serializable

data class LdapSettings(
    val enabled: Boolean = false,
    val hostname: String = "ldap.domain.com",
    val port: Int = 389,
    val baseDn: String = "dc=company,dc=com",
    val searchUserDn: String = "cn=reposilite,ou=admins,dc=domain,dc=com",
    val searchUserPassword: String = "reposilite-admin-secret",
    val userAttribute: String = "cn",
    val userFilter: String = "(&(objectClass=person)(ou=Maven Users))",
    val userType: UserType = UserType.PERSISTENT
) : Serializable {
    enum class UserType {
        PERSISTENT,
        TEMPORARY
    }
}
