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

import com.reposilite.Reposilite
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.ConfigurationService
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SettingsService
import com.reposilite.settings.api.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.api.createSharedConfigurationSchemaGenerator
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlConfigurationRepository
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.application.WebSettings

@Plugin(name = "settings")
internal class SettingsPlugin : ReposilitePlugin() {

    override fun initialize(): SettingsFacade {
        val reposilite = facade<Reposilite>()
        val parameters = reposilite.parameters
        val workingDirectory = parameters.workingDirectory
        val localConfiguration = reposilite.localConfiguration

        val configurationRepository = SqlConfigurationRepository(reposilite.database)
        val configurationService = ConfigurationService(this, workingDirectory, configurationRepository, reposilite.scheduler, localConfiguration)
        val settingsService = SettingsService(createSharedConfigurationSchemaGenerator())
        val settingsFacade = SettingsFacade(this, configurationService, settingsService)

        logger.info("")
        logger.info("--- Settings")

        settingsFacade.createConfigurationProvider(
            SharedConfiguration(),
            "Shared configuration",
            SHARED_CONFIGURATION_FILE,
            parameters.sharedConfigurationMode,
            parameters.sharedConfigurationPath
        )

        settingsFacade.createDomainSettings(WebSettings())

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(settingsFacade))
        }

        event { _: ReposiliteDisposeEvent ->
            settingsFacade.shutdownProviders()
        }

        return settingsFacade
    }

}
