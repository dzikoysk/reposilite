package com.reposilite.configuration.application

import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.ConfigurationRepository
import com.reposilite.configuration.infrastructure.InMemoryConfigurationRepository
import com.reposilite.configuration.infrastructure.SqlConfigurationRepository
import org.jetbrains.exposed.sql.Database

class ConfigurationComponents(private val database: Database? = null) {

    private fun configurationRepository(): ConfigurationRepository =
        when (database) {
            null -> InMemoryConfigurationRepository()
            else -> SqlConfigurationRepository(database)
        }

    fun configurationFacade(configurationRepository: ConfigurationRepository = configurationRepository()): ConfigurationFacade =
        ConfigurationFacade(configurationRepository)

}
