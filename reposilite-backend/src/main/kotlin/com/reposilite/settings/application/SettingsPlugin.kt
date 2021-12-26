package com.reposilite.settings.application

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import com.reposilite.settings.infrastructure.SqlSettingsRepository
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "settings")
internal class SettingsPlugin : ReposilitePlugin() {

    companion object {
        const val LOCAL_CONFIGURATION_FILE = "configuration.local.cdn"
        const val SHARED_CONFIGURATION_FILE = "configuration.shared.cdn"
    }

    override fun initialize(): SettingsFacade {
        val database = DatabaseSourceFactory.createConnection(extensions().parameters.workingDirectory, extensions().localConfiguration.database.get())

        val sharedConfigurationProvider = with(extensions().parameters) {
            if (sharedConfigurationMode == "none")
                SqlConfigurationProvider(
                    displayName = "Shared configuration",
                    journalist = this@SettingsPlugin,
                    settingsRepository = SqlSettingsRepository(database),
                    name = SHARED_CONFIGURATION_FILE,
                    configuration = SharedConfiguration()
                )
            else
                FileSystemConfigurationProvider(
                    displayName = "Shared configuration",
                    journalist = this@SettingsPlugin,
                    workingDirectory = workingDirectory,
                    defaultFileName = SHARED_CONFIGURATION_FILE,
                    configurationFile = sharedConfigurationPath,
                    mode = sharedConfigurationMode,
                    configuration = SharedConfiguration(),
                )
        }
        .also {
            logger.info("")
            logger.info("--- Settings")
            it.initialize()
        }

        val settingsFacade = SettingsFacade(extensions().localConfiguration, lazy { database }, sharedConfigurationProvider)

        event { event: ReposiliteInitializeEvent ->
            settingsFacade.registerWatchers(event.reposilite.scheduler)
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(settingsFacade))
        }

        event { _: ReposiliteDisposeEvent ->
            settingsFacade.shutdownProviders()
        }

        return settingsFacade
    }

}