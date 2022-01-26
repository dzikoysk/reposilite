package com.reposilite.settings.api

import java.io.Serializable

data class Settings (
    /** General settings */
    val general: GeneralSettings = GeneralSettings(),
    /** Advanced settings */
    val advanced: AdvancedSettings = AdvancedSettings(),
    /** List of Maven repositories. */
    val repositories: Map<String, RepositorySettings> = mapOf(
        "releases" to RepositorySettings(),
        "snapshots" to RepositorySettings(),
        "private" to RepositorySettings(visibility = RepositorySettings.Visibility.PRIVATE)
    ),
    /** Statistics module configuration. */
    val statistics: StatisticsSettings = StatisticsSettings()
) : Serializable
