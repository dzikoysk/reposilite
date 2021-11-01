package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsRepository
import java.time.Instant

internal class InMemorySettingsRepository : SettingsRepository {

    private val settings = HashMap<String, Pair<Instant, String>>()

    override fun saveConfiguration(name: String, configuration: String) {
        settings[name] = Pair(Instant.now(), configuration)
    }

    override fun findConfiguration(name: String): String? =
        settings[name]?.second

    override fun findConfigurationUpdateDate(name: String): Instant? =
        settings[name]?.first

}