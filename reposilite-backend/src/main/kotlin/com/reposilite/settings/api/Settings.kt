package com.reposilite.settings.api

import com.reposilite.auth.application.LdapSettings
import com.reposilite.frontend.application.AppearanceSettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.statistics.application.StatisticsSettings
import java.io.Serializable

data class Settings (
    /** Appearance settings */
    val appearance: AppearanceSettings,
    /** Advanced settings */
    val advanced: AdvancedSettings,
    /** List of Maven repositories. */
    val repositories: Map<String, RepositorySettings>,
    /** Statistics module configuration. */
    val statistics: StatisticsSettings,
    /** Ldap module settings */
    val ldap: LdapSettings
) : Serializable
