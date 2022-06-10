package com.reposilite.configuration

import com.reposilite.plugin.api.Facade
import java.time.Instant

class ConfigurationFacade internal constructor(
    private val configurationRepository: ConfigurationRepository
) : Facade {

    fun saveConfiguration(name: String, configuration: String) =
        configurationRepository.saveConfiguration(name, configuration)

    fun findConfiguration(name: String): String? =
        configurationRepository.findConfiguration(name)

    fun findConfigurationUpdateDate(name: String): Instant? =
        configurationRepository.findConfigurationUpdateDate(name)

}
