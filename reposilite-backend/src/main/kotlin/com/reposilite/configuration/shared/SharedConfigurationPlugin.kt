package com.reposilite.configuration.shared

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.parameters
import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.shared.infrastructure.LocalSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.RemoteSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.SettingsEndpoints
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.StorageProviderSettings
import com.reposilite.web.api.RoutingSetupEvent
import com.reposilite.web.application.WebSettings
import java.util.ServiceLoader

@Plugin(name = "shared-configuration", dependencies = ["failure", "configuration", "local-configuration"])
class SharedConfigurationPlugin : ReposilitePlugin() {

    override fun initialize(): SharedConfigurationFacade {
        val parameters = parameters()
        val failureFacade = facade<FailureFacade>()
        val configurationFacade = facade<ConfigurationFacade>()
        val sharedConfigurationFacade = SharedConfigurationFacade(createSharedConfigurationSchemaGenerator())

        logger.info("")
        logger.info("--- Settings")

        when (val sharedConfigurationFile = parameters.sharedConfigurationPath) {
            null -> configurationFacade.registerCustomConfigurationProvider(
                RemoteSharedConfigurationProvider(
                    journalist = this,
                    failureFacade = failureFacade,
                    configurationFacade = configurationFacade,
                    sharedConfigurationFacade = sharedConfigurationFacade
                )
            )
            else -> configurationFacade.registerCustomConfigurationProvider(
                LocalSharedConfigurationProvider(
                    journalist = this,
                    workingDirectory = parameters.workingDirectory,
                    configurationFile = sharedConfigurationFile
                )
            )
        }

        sharedConfigurationFacade.createDomainSettings(WebSettings())

        event { event: RoutingSetupEvent ->
            event.registerRoutes(SettingsEndpoints(sharedConfigurationFacade))
        }

        return sharedConfigurationFacade
    }

}

fun createSharedConfigurationSchemaGenerator(): SchemaGenerator {
    val storageProviders = ServiceLoader.load(StorageProviderFactory::class.java)
        .associate { it.settingsType to it.type }

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

    return createStandardSchemaGenerator(
        SettingsModule(
            subtypeResolvers = listOf(storageSubtypeResolver),
            enumResolvers = listOf(storageEnumResolver)
        )
    )
}