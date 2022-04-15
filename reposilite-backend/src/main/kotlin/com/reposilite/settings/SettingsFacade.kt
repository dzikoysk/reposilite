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
import com.reposilite.settings.api.SettingsHandler
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Supplier

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val configurationService: ConfigurationService,
    private val settingsService: SettingsService,
) : Facade {

    val localConfiguration: LocalConfiguration
        get() = configurationService.localConfiguration

    private val sharedConfiguration: SharedConfiguration
        get() = configurationService.findConfiguration()

    fun <C : Any> createConfigurationProvider(configuration: C, displayName: String, name: String, mode: String = "none", configurationFile: Path? = null): ConfigurationProvider<C> =
        configurationService.createConfigurationProvider(configuration, displayName, name, mode, configurationFile)

    fun <T : Any> createDomainSettings(settingsInstance : T): MutableReference<T> = mutableReference(settingsInstance).also {
        sharedConfiguration.domains[settingsInstance.javaClass] = it
        registerSettingsWatcher(settingsInstance.javaClass, it)
    }

    fun <T> getDomainSettings(settingsClass: Class<T>): MutableReference<T> = sharedConfiguration.forDomain(settingsClass)

    inline fun <reified T> getDomainSettings(): MutableReference<T> = getDomainSettings(T::class.java)

    fun <T> registerSettingsWatcher(handler: SettingsHandler<T>) =
        settingsService.registerSettingsWatcher(handler)

    fun <T> registerSettingsWatcher(type: Class<T>, getter: Supplier<T>, setter: Consumer<T>) =
        settingsService.registerSettingsWatcher(type, getter, setter)

    fun <T> registerSettingsWatcher(type: Class<T>, reference: MutableReference<T>) =
        settingsService.registerSettingsWatcher(type, reference)

    fun updateSettings(name: String, body: Any): Result<Any, ErrorResponse> =
        settingsService.updateSettings(name, body)

    fun getSettings(name: String): Result<Any, ErrorResponse> =
        settingsService.getSettings(name)

    fun getSchema(name: String): Result<JsonNode, ErrorResponse> =
        settingsService.getSchema(name)

    fun getSettingsClass(name: String): Result<Class<*>, ErrorResponse> =
        settingsService.getClass(name)

    fun listSettings(): Result<Map<String, String>, ErrorResponse> =
        Result.ok(settingsService.listSettings())

    fun shutdownProviders() =
        configurationService.shutdownProviders()
}
