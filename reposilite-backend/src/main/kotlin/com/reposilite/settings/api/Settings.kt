package com.reposilite.settings.api

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
