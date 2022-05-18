package com.reposilite.configuration

import com.fasterxml.jackson.databind.JsonNode
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.ReposiliteParameters
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.SharedSettings
import com.reposilite.configuration.shared.infrastructure.SHARED_CONFIGURATION_FILE
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import java.nio.file.Files
import java.util.ServiceLoader
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

// This is slightly dirty, but it's a side-case feature
// Maybe we'll find out a better solution in the future
internal fun generateRequestedConfiguration(parameters: ReposiliteParameters) {
    when (parameters.configurationRequested?.lowercase()) {
        "local", "configuration.cdn" -> {
            val localConfigurationFile = parameters.workingDirectory.resolve(LOCAL_CONFIGURATION_FILE)
            KCdnFactory.createStandard().render(LocalConfiguration(), Source.of(localConfigurationFile))
            println("Local configuration has been generated to the $localConfigurationFile file")
        }
        "shared", "shared.configuration.json" -> {
            val sharedConfigurationFile = parameters.workingDirectory.resolve(SHARED_CONFIGURATION_FILE)
            ServiceLoader.load(ReposilitePlugin::class.java).asSequence()
                .mapNotNull { it::class.findAnnotation<Plugin>() }
                .associate { it.name to it.settings }
                .filter { (_, type) -> type != SharedSettings::class }
                .mapValues { (_, type) -> type.createInstance() }
                .let { ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER.valueToTree<JsonNode>(it) }
                .toPrettyString()
                .also { Files.writeString(sharedConfigurationFile, it) }
            println("Shared configuration has been generated to the $sharedConfigurationFile file")
        }
        else -> println("Unknown configuration: ${parameters.configurationRequested}")
    }
}