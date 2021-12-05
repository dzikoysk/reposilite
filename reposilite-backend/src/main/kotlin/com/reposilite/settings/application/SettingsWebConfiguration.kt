package com.reposilite.settings.application

import com.reposilite.Reposilite
import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.Journalist
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import com.reposilite.settings.infrastructure.SqlSettingsRepository
import com.reposilite.web.WebConfiguration
import com.reposilite.web.application.ReposiliteRoutes
import org.jetbrains.exposed.sql.Database

internal object SettingsWebConfiguration : WebConfiguration {

    const val LOCAL_CONFIGURATION_FILE = "configuration.local.cdn"
    const val SHARED_CONFIGURATION_FILE = "configuration.shared.cdn"

    fun createLocalConfiguration(journalist: Journalist, parameters: ReposiliteParameters): LocalConfiguration =
        FileSystemConfigurationProvider(
            "Local configuration",
            journalist,
            parameters.workingDirectory,
            LOCAL_CONFIGURATION_FILE,
            parameters.localConfigurationMode,
            parameters.localConfigurationPath,
            LocalConfiguration()
        )
        .also { it.initialize() }
        .configuration

    fun createFacade(journalist: Journalist, parameters: ReposiliteParameters, localConfiguration: LocalConfiguration, database: Database): SettingsFacade {
        val sharedConfigurationProvider =
            if (parameters.sharedConfigurationMode == "none")
                SqlConfigurationProvider(
                    displayName = "Shared configuration",
                    journalist = journalist,
                    settingsRepository = SqlSettingsRepository(database),
                    name = SHARED_CONFIGURATION_FILE,
                    configuration = SharedConfiguration()
                )
            else
                FileSystemConfigurationProvider(
                    displayName = "Shared configuration",
                    journalist = journalist,
                    workingDirectory = parameters.workingDirectory,
                    defaultFileName = SHARED_CONFIGURATION_FILE,
                    configurationFile = parameters.sharedConfigurationPath,
                    mode = parameters.sharedConfigurationMode,
                    configuration = SharedConfiguration(),
                )
        sharedConfigurationProvider.initialize()

        return SettingsFacade(localConfiguration, sharedConfigurationProvider)
    }

    override fun initialize(reposilite: Reposilite) {
        reposilite.settingsFacade.registerWatchers(reposilite.scheduler)
    }

    override fun routing(reposilite: Reposilite): Set<ReposiliteRoutes> = setOf(
        SettingsEndpoints(reposilite.settingsFacade )
    )

    override fun dispose(reposilite: Reposilite) {
        reposilite.settingsFacade.shutdownProviders()
    }

}