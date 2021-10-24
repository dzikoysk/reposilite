package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsRepository
import java.time.Instant

internal class InMemorySettingsRepository : SettingsRepository {

    private var configuration: String = ""
    private var updateDate: Instant = Instant.ofEpochMilli(0)

    override fun saveSharedConfiguration(configuration: String) {
        this.configuration = configuration
    }

    override fun findSharedConfiguration(): String =
        configuration

    override fun findSharedConfigurationUpdateDate(): Instant =
        updateDate

}