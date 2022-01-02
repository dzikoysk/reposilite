package com.reposilite.settings

import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import org.jetbrains.exposed.sql.Database
import panda.std.Result
import panda.std.Unit
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    val localConfiguration: LocalConfiguration,
    val database: Lazy<Database>,
    private val settingsRepository: SettingsRepository,
) : Facade {

    private val configurationProviders = mutableMapOf<String, ConfigurationProvider<*>>()

    val sharedConfiguration: SharedConfiguration // expose it directly for easier calls
        get() = findConfiguration()

    fun <C : Any> createConfigurationProvider(configuration: C, displayName: String, name: String, mode: String = "none", configurationFile: Path? = null): ConfigurationProvider<C> =
        registerCustomConfigurationProvider(
            if (mode == "none")
                SqlConfigurationProvider(
                    name = name,
                    displayName = displayName,
                    journalist = journalist,
                    settingsRepository = settingsRepository,
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

    fun <C : Any> registerCustomConfigurationProvider(configurationProvider: ConfigurationProvider<C>): ConfigurationProvider<C> {
        configurationProviders[configurationProvider.name] = configurationProvider
        configurationProvider.initialize()
        return configurationProvider
    }

    internal fun attachWatcherScheduler(scheduler: ScheduledExecutorService) =
        configurationProviders.forEach { (_, provider) -> provider.registerWatcher(scheduler) }

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        configurationProviders[name]?.resolve(name) ?: notFoundError("Configuration $name not found")

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        configurationProviders[request.name]?.update(request) ?: notFoundError("Configuration ${request.name} not found")

    fun shutdownProviders() =
        configurationProviders.forEach { (_, provider) -> provider.shutdown() }

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> findConfiguration(type: Class<C>): C =
        configurationProviders.values
            .first { type.isInstance(it.configuration) }
            .configuration as C

    inline fun <reified C : Any> findConfiguration(): C =
        findConfiguration(C::class.java)

}