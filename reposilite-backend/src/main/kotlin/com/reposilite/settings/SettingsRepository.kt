package com.reposilite.settings

import java.time.Instant

internal interface SettingsRepository {

    fun saveConfiguration(name: String, configuration: String)

    fun findConfiguration(name: String): String?

    fun findConfigurationUpdateDate(name: String): Instant?

}