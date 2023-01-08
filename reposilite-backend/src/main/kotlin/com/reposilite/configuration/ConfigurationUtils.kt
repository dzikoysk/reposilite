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

package com.reposilite.configuration

import com.fasterxml.jackson.databind.JsonNode
import com.reposilite.ReposiliteObjectMapper
import com.reposilite.ReposiliteParameters
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.api.SharedSettings
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
        "shared", "configuration.shared.json" -> {
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
