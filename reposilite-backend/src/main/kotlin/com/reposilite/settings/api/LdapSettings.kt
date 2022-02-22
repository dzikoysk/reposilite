package com.reposilite.settings.api

import java.io.Serializable

data class LdapSettings(
    val enabled: Boolean,
    val hostname: String,
    val port: Int,
    val baseDn: String,
    val searchUserDn: String,
    val searchUserPassword: String,
    val userAttribute: String,
    val userFilter: String,
    val userType: UserType
) : Serializable {
    enum class UserType {
        PERSISTENT,
        TEMPORARY
    }
}
