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

import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.settings.EnumResolver
import com.reposilite.settings.SHARED_CONFIGURATION_FILE
import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.SettingsModule
import com.reposilite.web.application.WebSettings
import com.reposilite.settings.api.Settings
import com.reposilite.settings.SharedConfiguration
import com.reposilite.settings.SubtypeResolver
import com.reposilite.settings.createStandardSchemaGenerator
import com.reposilite.settings.infrastructure.SettingsEndpoints
import com.reposilite.settings.infrastructure.SqlSettingsRepository
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.application.StorageProviderSettings
import com.reposilite.web.api.RoutingSetupEvent
import java.util.ServiceLoader

@Plugin(name = "settings")
internal class SettingsPlugin : ReposilitePlugin() {

    override fun initialize(): SettingsFacade {
        val parameters = extensions().parameters
        val workingDirectory = parameters.workingDirectory
        val localConfiguration = extensions().localConfiguration

        val database = DatabaseSourceFactory.createConnection(workingDirectory, localConfiguration.database.get(), localConfiguration.databaseThreadPool.get())
        val settingsRepository = SqlSettingsRepository(database)

        val storageProviders = ServiceLoader.load(StorageProviderFactory::class.java).associate { it.settingsType to it.type }

        val storageEnumResolver = EnumResolver {
            if (it.name == "type")
                storageProviders[it.declaringType.erasedType]?.let { type -> listOf(type) }
            else null
        }
        val storageSubtypeResolver = SubtypeResolver { declaredType, context ->
            if (declaredType.erasedType == StorageProviderSettings::class.java)
                storageProviders.keys.toList().map { clazz -> context.typeContext.resolveSubtype(declaredType, clazz) }
            else null
        }

        val schemaGenerator = createStandardSchemaGenerator(SettingsModule(
            subtypeResolvers =  listOf(storageSubtypeResolver),
            enumResolvers = listOf(storageEnumResolver)
        ))

        val settingsFacade = SettingsFacade(this, workingDirectory, localConfiguration, lazy { database }, settingsRepository, schemaGenerator)

        logger.info("")
        logger.info("--- Settings")

        settingsFacade.createConfigurationProvider(
            SharedConfiguration(),
            "Shared configuration",
            SHARED_CONFIGURATION_FILE,
            parameters.sharedConfigurationMode,
            parameters.sharedConfigurationPath
        )

        settingsFacade.registerSchemaWatcher(
            WebSettings::class.java,
            { settingsFacade.sharedConfiguration.web.get() },
            { settingsFacade.sharedConfiguration.web.update(it) }
        )

        settingsFacade.registerSchemaWatcher(
            Settings::class.java,
            settingsFacade.sharedConfiguration::toDto,
            settingsFacade.sharedConfiguration::update
        )

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
