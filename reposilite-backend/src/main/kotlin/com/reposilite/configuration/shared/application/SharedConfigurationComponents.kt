package com.reposilite.configuration.shared.application

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_7
import com.reposilite.configuration.ConfigurationFacade
import com.reposilite.configuration.shared.EnumResolver
import com.reposilite.configuration.shared.SCHEMA_OPTION_PRESET
import com.reposilite.configuration.shared.SettingsModule
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.configuration.shared.SharedConfigurationProvider
import com.reposilite.configuration.shared.SharedSettingsProvider
import com.reposilite.configuration.shared.SubtypeResolver
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.configuration.shared.infrastructure.LocalSharedConfigurationProvider
import com.reposilite.configuration.shared.infrastructure.RemoteSharedConfigurationProvider
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.PluginComponents
import com.reposilite.status.FailureFacade
import java.nio.file.Path
import java.util.ServiceLoader

class SharedConfigurationComponents(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    private val extensions: Extensions,
    private val sharedConfigurationPath: Path?,
    private val failureFacade: FailureFacade,
    private val configurationFacade: ConfigurationFacade,
) : PluginComponents {

    private fun sharedSettingsProvider(): SharedSettingsProvider =
        extensions.getPlugins().values
            .map { it.metadata.settings }
            .filter { it != SharedSettings::class }
            .let { SharedSettingsProvider.createStandardProvider(it) }

    private fun sharedConfigurationProvider(): SharedConfigurationProvider =
        when (val sharedConfigurationFile = sharedConfigurationPath) {
            null ->
                RemoteSharedConfigurationProvider(
                    configurationFacade = configurationFacade,
                )
            else ->
                LocalSharedConfigurationProvider(
                    journalist = journalist,
                    workingDirectory = workingDirectory,
                    configurationFile = sharedConfigurationFile,
                )
        }

    private fun schemaGenerator(): SchemaGenerator =
        SchemaGenerator(
            SchemaGeneratorConfigBuilder(DRAFT_7, SCHEMA_OPTION_PRESET)
                .with(
                    SettingsModule(
                        subtypeResolvers = ServiceLoader.load(SubtypeResolver::class.java).toList(),
                        enumResolvers = ServiceLoader.load(EnumResolver::class.java).toList()
                    )
                )
                .build()
        )

    fun sharedConfigurationFacade(
        schemaGenerator: SchemaGenerator = schemaGenerator(),
        sharedSettingsProvider: SharedSettingsProvider = sharedSettingsProvider(),
        sharedConfigurationProvider: SharedConfigurationProvider = sharedConfigurationProvider()
    ): SharedConfigurationFacade =
        SharedConfigurationFacade(
            journalist = journalist,
            schemaGenerator = schemaGenerator,
            failureFacade = failureFacade,
            sharedSettingsProvider = sharedSettingsProvider,
            sharedConfigurationProvider = sharedConfigurationProvider
        )

}