package com.reposilite.settings

import java.time.Instant

internal interface SettingsRepository {

    fun saveSharedConfiguration(configuration: String)

    fun findSharedConfiguration(): String

    fun findSharedConfigurationUpdateDate(): Instant

}