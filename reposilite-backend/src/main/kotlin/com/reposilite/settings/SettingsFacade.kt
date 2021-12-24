package com.reposilite.settings

import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.Unit
import java.util.concurrent.ScheduledExecutorService

class SettingsFacade internal constructor(
    val localConfiguration: LocalConfiguration,
    private val sharedConfigurationProvider: ConfigurationProvider<SharedConfiguration>
) : Facade {

    val sharedConfiguration: SharedConfiguration // expose it directly for easier calls
        get() = sharedConfigurationProvider.configuration

    fun registerWatchers(scheduler: ScheduledExecutorService) =
        sharedConfigurationProvider.registerWatcher(scheduler)

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        sharedConfigurationProvider.resolve(name)

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        sharedConfigurationProvider.update(request)

    fun shutdownProviders() {
        sharedConfigurationProvider.shutdown()
    }

}