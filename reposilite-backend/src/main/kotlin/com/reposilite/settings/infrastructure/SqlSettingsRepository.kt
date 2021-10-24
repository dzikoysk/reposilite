package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsRepository
import java.time.Instant

internal class SqlSettingsRepository : SettingsRepository {

    override fun saveSharedConfiguration(configuration: String) {
        TODO("Not yet implemented")
    }

    override fun findSharedConfiguration(): String {
        TODO("Not yet implemented")
    }

    override fun findSharedConfigurationUpdateDate(): Instant {
        TODO("Not yet implemented")
    }

}