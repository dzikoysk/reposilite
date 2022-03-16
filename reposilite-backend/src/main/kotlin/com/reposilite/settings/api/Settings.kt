package com.reposilite.settings.api

import com.reposilite.auth.application.LdapSettings
import com.reposilite.frontend.application.AppearanceSettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.statistics.application.StatisticsSettings
import com.reposilite.web.application.WebSettings
import java.io.Serializable

data class Settings (
    /** Appearance settings */
    @Doc(title = "Appearance", description = "Appearance settings")
    val appearance: AppearanceSettings,
    /** Advanced settings */
    @Doc(title = "Advanced", description = "Advanced settings")
    val advanced: WebSettings,
    /** List of Maven repositories */
    @Doc(title = "Repositories", description = "List of Maven repositories.")
    val repositories: Map<String, RepositorySettings>,
    /** Statistics settings */
    @Doc(title = "Statistics", description = "Statistics settings")
    val statistics: StatisticsSettings,
    /** LDAP settings */
    @Doc(title = "LDAP", description = "LDAP settings")
    val ldap: LdapSettings
) : Serializable
