package com.reposilite.configuration.shared

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.shared.infrastructure.LocalSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.RemoteSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.SettingsEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteDisposeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.plugin.reposilite
import com.reposilite.status.FailureFacade
import com.reposilite.web.api.RoutingSetupEvent
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit

@Plugin(name = "shared-configuration", dependencies = ["failure", "configuration", "local-configuration"])
class SharedConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): SharedConfigurationFacade {
        val parameters = parameters()
        val failureFacade = facade<FailureFacade>()
        val configurationFacade = facade<ConfigurationFacade>()

        val sharedSettingsProvider = extensions().getPlugins().values
            .map { it.metadata.settings }
            .filter { it != SharedSettings::class }
            .let { SharedSettingsProvider.createStandardProvider(it) }

        val sharedConfigurationProvider = when (val sharedConfigurationFile = parameters.sharedConfigurationPath) {
            null ->
                RemoteSharedConfigurationProvider(
                    configurationFacade = configurationFacade,
                )
            else ->
                LocalSharedConfigurationProvider(
                    journalist = this,
                    workingDirectory = parameters.workingDirectory,
                    configurationFile = sharedConfigurationFile,
                )
        }

        val sharedConfigurationFacade = SharedConfigurationFacade(
            journalist = this,
            schemaGenerator = createSharedConfigurationSchemaGenerator(),
            failureFacade = failureFacade,
            sharedSettingsProvider = sharedSettingsProvider,
            sharedConfigurationProvider = sharedConfigurationProvider
        )

        logger.info("Shared Configuration | Loading shared configuration from ${sharedConfigurationProvider.name()}")
        sharedConfigurationFacade.loadSharedSettingsFromString(sharedConfigurationProvider.fetchConfiguration())

        if (sharedConfigurationProvider.isMutable()) {
            val watcher = reposilite().scheduler.scheduleWithFixedDelay({
                if (!sharedConfigurationProvider.isUpdateRequired()) {
                    return@scheduleWithFixedDelay
                }

                logger.info("Shared Configuration | Propagation | Shared configuration has been changed in ${sharedConfigurationProvider.name()}, updating current instance...")
                sharedConfigurationFacade.loadSharedSettingsFromString(sharedConfigurationProvider.fetchConfiguration())
                logger.info("Shared Configuration | Propagation | Sources have been updated successfully")
            }, 10, 10, TimeUnit.SECONDS)

            event { _: ReposiliteDisposeEvent ->
                watcher.cancel(false)
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(sharedConfigurationFacade))
        }

        return sharedConfigurationFacade
    }

}

fun createSharedConfigurationSchemaGenerator(): SchemaGenerator =
    createStandardSchemaGenerator(
        SettingsModule(
            subtypeResolvers = ServiceLoader.load(SubtypeResolver::class.java).toList(),
            enumResolvers = ServiceLoader.load(EnumResolver::class.java).toList()
        )
    )