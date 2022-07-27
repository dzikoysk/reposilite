package com.reposilite.configuration.shared.application

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
        sharedConfigurationFacade.loadSharedSettingsFromString(sharedConfigurationFacade.fetchConfiguration())

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

        sharedConfigurationFacade.getDomainNames()
            .map { sharedConfigurationFacade.getSettingsReference<SharedSettings>(it) }
            .forEach {
                logger.debug("Schema for ${it?.name}:")
                logger.debug(it?.schema?.toPrettyString())
            }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(sharedConfigurationFacade))
        }

        return sharedConfigurationFacade
    }

}
