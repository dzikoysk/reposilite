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

package com.reposilite.configuration.local

import com.reposilite.ReposiliteParameters
import com.reposilite.configuration.local.infrastructure.LocalConfigurationProvider
import com.reposilite.journalist.Journalist
import panda.std.reactive.Reference
import panda.std.reactive.ReferenceUtils
import java.lang.IllegalArgumentException
import kotlin.reflect.full.declaredMemberProperties

internal object LocalConfigurationFactory {

    fun createLocalConfiguration(journalist: Journalist?, parameters: ReposiliteParameters): LocalConfiguration =
        LocalConfigurationProvider(
            journalist = journalist,
            workingDirectory = parameters.workingDirectory,
            configurationFile = parameters.localConfigurationPath,
            mode = parameters.localConfigurationMode,
            localConfiguration = LocalConfiguration()
        ).also { provider ->
            provider.initialize()
            loadCustomPropertiesViaReflections(journalist, provider.localConfiguration)
        }.localConfiguration

    /**
     * Load custom properties from environment variables and system properties.
     */
    private fun loadCustomPropertiesViaReflections(journalist: Journalist?, localConfiguration: LocalConfiguration) {
        (getEnvironmentVariables() + getProperties()).forEach { (key, value) ->
            val property = localConfiguration::class.declaredMemberProperties.find { it.name.equals(key, ignoreCase = true) } ?: run {
                journalist?.logger?.warn("Unknown local configuration property: $key")
                return@forEach
            }

            @Suppress("UNCHECKED_CAST")
            val reference = property.getter.call(localConfiguration) as Reference<Any>

            when (reference.type.kotlin) {
                Boolean::class -> ReferenceUtils.setValue(reference, value.toBoolean())
                Int::class -> ReferenceUtils.setValue(reference, value.toInt())
                String::class -> ReferenceUtils.setValue(reference, value)
                else -> throw IllegalArgumentException("Unsupported local configuration property type: $key (expected: ${reference.type})")
            }

            journalist?.logger?.info("Local configuration has been updated by external property: $key=${value.take(1)}***${value.takeLast(1)}")
        }
    }

    /**
     * Get all environment variables that starts with REPOSILITE.LOCAL., example:
     * REPOSILITE.LOCAL.SSLENABLED=false
     */
    private fun getEnvironmentVariables(): Map<String, String> =
        System.getenv()
            .asSequence()
            .map { it.key.uppercase() to it.value }
            .filter { (key) -> key.startsWith("REPOSILITE.LOCAL.") }
            .associate { (key, value) -> key.substringAfter("REPOSILITE.LOCAL.") to value }

    /**
     * Get all system properties that starts with reposilite.local., example:
     * reposilite.local.sslEnabled=false
     */
    private fun getProperties(): Map<String, String> =
        System.getProperties()
            .propertyNames()
            .asSequence()
            .map { it.toString() }
            .filter { it.lowercase().startsWith("reposilite.local.") }
            .associate { it.lowercase().substringAfter("reposilite.local.") to System.getProperty(it) }

}
