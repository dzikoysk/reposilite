package com.reposilite.configuration.shared

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.shared.infrastructure.LocalSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.RemoteSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.SettingsEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.status.FailureFacade
import com.reposilite.web.api.RoutingSetupEvent
import java.util.ServiceLoader
import kotlin.reflect.full.createInstance

@Plugin(name = "shared-configuration", dependencies = ["failure", "configuration", "local-configuration"])
class SharedConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): SharedConfigurationFacade {
        val parameters = parameters()
        val failureFacade = facade<FailureFacade>()
        val configurationFacade = facade<ConfigurationFacade>()

        logger.info("")
        logger.info("--- Settings")

        val sharedConfigurationFacade = SharedConfigurationFacade(
            journalist = this,
            schemaGenerator = createSharedConfigurationSchemaGenerator(),
            failureFacade = failureFacade
        )

        extensions().getPlugins().values
            .map { it.metadata.settings }
            .filter { it != SharedSettings::class }
            .forEach { sharedConfigurationFacade.createDomainSettings(it.createInstance()) }

        when (val sharedConfigurationFile = parameters.sharedConfigurationPath) {
            null -> configurationFacade.registerCustomConfigurationProvider(
                RemoteSharedConfigurationProvider(
                    journalist = this,
                    configurationFacade = configurationFacade,
                    sharedConfigurationFacade = sharedConfigurationFacade
                )
            )
            else -> configurationFacade.registerCustomConfigurationProvider(
                LocalSharedConfigurationProvider(
                    journalist = this,
                    workingDirectory = parameters.workingDirectory,
                    configurationFile = sharedConfigurationFile,
                    sharedConfigurationFacade = sharedConfigurationFacade
                )
            )
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