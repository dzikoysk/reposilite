package com.reposilite.configuration

import com.reposilite.plugin.api.Facade
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService

class ConfigurationFacade internal constructor(
    private val configurationRepository: ConfigurationRepository,
    private val scheduler: ScheduledExecutorService
) : Facade {

    private val configurationProviders = mutableMapOf<String, ConfigurationProvider>()

    fun registerCustomConfigurationProvider(configurationProvider: ConfigurationProvider): ConfigurationProvider =
        configurationProvider.also {
            configurationProviders[it.name] = it
            it.initialize()
            it.registerWatcher(scheduler)
        }

    fun saveConfiguration(name: String, configuration: String) =
        configurationRepository.saveConfiguration(name, configuration)

    fun findConfiguration(name: String): String? =
        configurationRepository.findConfiguration(name)

    fun findConfigurationUpdateDate(name: String): Instant? =
        configurationRepository.findConfigurationUpdateDate(name)

    fun shutdownProviders() =
        configurationProviders.forEach { (_, provider) -> provider.shutdown() }

}