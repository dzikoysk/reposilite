/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.configuration.shared.application

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.configuration.shared.infrastructure.SettingsEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.plugin.reposilite
import com.reposilite.web.api.RoutingSetupEvent
import java.util.concurrent.TimeUnit

@Plugin(name = "shared-configuration", dependencies = ["failure", "configuration", "local-configuration"])
class SharedConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): SharedConfigurationFacade {
        val localConfiguration = facade<LocalConfiguration>()

        val sharedConfigurationFacade = SharedConfigurationComponents(
            journalist = this,
            workingDirectory = parameters().workingDirectory,
            extensions = extensions(),
            sharedConfigurationPath = parameters().sharedConfigurationPath,
            failureFacade = facade(),
            configurationFacade = facade()
        ).sharedConfigurationFacade()

        logger.info("")
        logger.info("--- Shared settings")
        logger.info("Loading shared configuration from ${sharedConfigurationFacade.getProviderName()}")
        val storedConfiguration = sharedConfigurationFacade.fetchConfiguration()
        val loadResult = sharedConfigurationFacade.loadSharedSettingsFromString(storedConfiguration)

        if (loadResult.isErr && !parameters().ignoreSharedConfigurationErrors) {
            logger.error("Failed to load shared configuration from '${sharedConfigurationFacade.getProviderName()}' provider.")
            logger.error("Please check your configuration and try again.")
            logger.error("If you want to ignore those errors and let Reposilite start with default settings as a fallback values,")
            logger.error("please launch Reposilite with --ignore-shared-configuration-errors' flag.")
            throw loadResult.error
        }

        if (sharedConfigurationFacade.isMutable()) {
            val watcher = reposilite().scheduler.scheduleWithFixedDelay({
                if (!sharedConfigurationFacade.isUpdateRequired()) {
                    return@scheduleWithFixedDelay
                }

                logger.info("Propagation | Shared configuration has been changed in ${sharedConfigurationFacade.getProviderName()}, updating current instance...")
                sharedConfigurationFacade.loadSharedSettingsFromString(sharedConfigurationFacade.fetchConfiguration())
                logger.info("Propagation | Sources have been updated successfully")
            }, 10, 10, TimeUnit.SECONDS)

            event { _: ReposiliteDisposeEvent ->
                watcher.cancel(false)
            }
        }

        if (localConfiguration.debugEnabled.get()) {
            sharedConfigurationFacade.getDomainNames()
                .map { sharedConfigurationFacade.getSettingsReference<SharedSettings>(it) }
                .forEach {
                    logger.debug("Schema for ${it?.name}:")
                    logger.debug(it?.schema?.get()?.reader()?.readText())
                }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(sharedConfigurationFacade))
        }

        return sharedConfigurationFacade
    }

}
