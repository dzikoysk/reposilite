package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

class ConfigurationService internal constructor(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val configurationRepository: ConfigurationRepository,
    private val scheduler: ScheduledExecutorService,
    val localConfiguration: LocalConfiguration,
) {

    private val configurationProviders = mutableMapOf<String, ConfigurationProvider<*>>()

    fun <C : Any> createConfigurationProvider(configuration: C, displayName: String, name: String, mode: String = "none", configurationFile: Path? = null): ConfigurationProvider<C> =
        registerCustomConfigurationProvider(
            if (mode == "none")
                SqlConfigurationProvider(
                    name = name,
                    displayName = displayName,
                    journalist = journalist,
                    configurationRepository = configurationRepository,
                    configuration = configuration
                )
            else
                FileSystemConfigurationProvider(
                    name = name,
                    displayName = displayName,
                    journalist = journalist,
                    workingDirectory = workingDirectory,
                    configurationFile = configurationFile ?: workingDirectory.resolve(name),
                    mode = mode,
                    configuration = configuration,
                )
        )

    fun <C : Any> registerCustomConfigurationProvider(configurationProvider: ConfigurationProvider<C>): ConfigurationProvider<C> =
        configurationProvider.also {
            configurationProviders[it.name] = it
            it.initialize()
            it.registerWatcher(scheduler)
        }

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> findConfiguration(type: Class<C>): C =
        configurationProviders.values
            .first { type.isInstance(it.configuration) }
            .configuration as C

    inline fun <reified C : Any> findConfiguration(): C =
        findConfiguration(C::class.java)

    fun shutdownProviders() =
        configurationProviders.forEach { (_, provider) -> provider.shutdown() }

}