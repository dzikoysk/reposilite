package com.reposilite.settings

import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.Journalist
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.application.SettingsWebConfiguration
import com.reposilite.web.http.ErrorResponse
import panda.std.Result

class SettingsFacade internal constructor(
    val localConfiguration: LocalConfiguration,
    private val sharedConfigurationService: SharedConfigurationService
) {

    val sharedConfiguration: SharedConfiguration
        get() = sharedConfigurationService.sharedConfiguration

    fun synchronizeConfigurations() =
        sharedConfigurationService.synchronizeSharedConfiguration()

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        sharedConfigurationService.resolveConfiguration(name)

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        sharedConfigurationService.updateConfiguration(request)

    companion object {

        fun createLocalConfiguration(journalist: Journalist, parameters: ReposiliteParameters): LocalConfiguration =
            SettingsFileLoader.initializeAndLoad(
                journalist,
                parameters.localConfigurationMode,
                parameters.localConfigurationPath,
                parameters.workingDirectory,
                SettingsWebConfiguration.LOCAL_CONFIGURATION_FILE,
                LocalConfiguration()
            )

    }

}