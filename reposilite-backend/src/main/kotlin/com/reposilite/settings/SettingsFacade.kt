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

package com.reposilite.settings

import com.fasterxml.jackson.databind.JsonNode
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.reactive.MutableReference
import java.nio.file.Path

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val configurationService: ConfigurationService,
    private val schemaService: SchemaService,
) : Facade {

    val localConfiguration: LocalConfiguration
        get() = configurationService.localConfiguration

    val sharedConfiguration: SharedConfiguration // expose it directly for easier calls
        get() = configurationService.findConfiguration()

    fun <C : Any> createConfigurationProvider(configuration: C, displayName: String, name: String, mode: String = "none", configurationFile: Path? = null): ConfigurationProvider<C> =
        configurationService.createConfigurationProvider(configuration, displayName, name, mode, configurationFile)

    fun <T> registerSchemaWatcher(type: Class<T>, reference: MutableReference<T>) =
        schemaService.registerSchemaWatcher(type, reference)

    fun updateConfiguration(name: String, body: Any): Result<Any, ErrorResponse> =
        schemaService.updateConfiguration(name, body)

    fun getConfiguration(name: String): Result<Any, ErrorResponse> =
        schemaService.getConfiguration(name)

    fun getSchema(name: String): Result<JsonNode, ErrorResponse> =
        schemaService.getHandler(name).map { it.schema }

    fun getSettingsClassForName(name: String): Result<Class<*>, ErrorResponse> =
        schemaService.getSettingsClassForName(name)

    fun shutdownProviders() =
        configurationService.shutdownProviders()

}