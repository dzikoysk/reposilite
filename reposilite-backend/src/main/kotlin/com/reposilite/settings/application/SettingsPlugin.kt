/*
 * Copyright (c) 2022 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.settings.application

import com.reposilite.maven.application.RepositoriesSettings
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.AdvancedSettings
import com.reposilite.settings.api.Settings
import com.reposilite.settings.api.SettingsHandler
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlSettingsRepository
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "settings")
internal class SettingsPlugin : ReposilitePlugin() {

    companion object {
        const val LOCAL_CONFIGURATION_FILE = "configuration.local.cdn"
        const val SHARED_CONFIGURATION_FILE = "configuration.shared.cdn"
    }

    override fun initialize(): SettingsFacade {
        val parameters = extensions().parameters
        val workingDirectory = parameters.workingDirectory
        val localConfiguration = extensions().localConfiguration

        val database = DatabaseSourceFactory.createConnection(workingDirectory, localConfiguration.database.get())
        val settingsRepository = SqlSettingsRepository(database)
        val settingsFacade = SettingsFacade(this, workingDirectory, localConfiguration, lazy { database }, settingsRepository)

        logger.info("")
        logger.info("--- Settings")

        settingsFacade.createConfigurationProvider(
            SharedConfiguration(),
            "Shared configuration",
            SHARED_CONFIGURATION_FILE,
            parameters.sharedConfigurationMode,
            parameters.sharedConfigurationPath
        )

        settingsFacade.registerHandler(
            SettingsHandler.of(
            "advanced",
            AdvancedSettings::class.java,
            { settingsFacade.sharedConfiguration.advanced.get() },
            { settingsFacade.sharedConfiguration.advanced.update(it) }
        ))
        settingsFacade.registerHandler(SettingsHandler.of("all", Settings::class.java, settingsFacade.sharedConfiguration::getSettingsDTO, settingsFacade.sharedConfiguration::updateFromSettingsDTO))

        event { event: ReposiliteInitializeEvent ->
            settingsFacade.attachWatcherScheduler(event.reposilite.scheduler)
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

private fun SharedConfiguration.updateFromSettingsDTO(settings: Settings): Settings {
    repositories.update(RepositoriesSettings(settings.repositories))
    advanced.update(settings.advanced)
    appearance.update(settings.appearance)
    statistics.update(settings.statistics)
    ldap.update(settings.ldap)
    return getSettingsDTO()
}

private fun SharedConfiguration.getSettingsDTO(): Settings = Settings(appearance = appearance.get(), advanced = advanced.get(), repositories = repositories.get().repositories, statistics = statistics.get(), ldap = ldap.get())

