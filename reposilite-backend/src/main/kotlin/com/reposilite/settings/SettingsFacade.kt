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
import com.reposilite.settings.api.Settings
import com.reposilite.settings.api.SettingsHandler
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.reactive.MutableReference
import panda.std.reactive.mutableReference
import java.nio.file.Path

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

    fun <S : Settings> createDomainSettings(settingsInstance : S): MutableReference<S> = mutableReference(settingsInstance).also {
        sharedConfiguration.domains[settingsInstance.javaClass] = it
        registerSettingsWatcher(settingsInstance.javaClass, it)
    }

    fun <S> getDomainSettings(settingsClass: Class<S>): MutableReference<S> =
        sharedConfiguration.forDomain(settingsClass)

    inline fun <reified T> getDomainSettings(): MutableReference<T> =
        getDomainSettings(T::class.java)

    fun <S : Settings> registerSettingsWatcher(handler: SettingsHandler<S>): SettingsHandler<S> =
        settingsService.registerSettingsWatcher(handler)

    fun <S : Settings> registerSettingsWatcher(type: Class<S>, getter: () -> S, setter: (S) -> Unit): SettingsHandler<S> =
        settingsService.registerSettingsWatcher(type, getter, setter)

    fun <S : Settings> registerSettingsWatcher(type: Class<S>, reference: MutableReference<S>): SettingsHandler<S> =
        settingsService.registerSettingsWatcher(type, reference)

    fun <S : Settings> updateSettings(name: String, body: S): Result<S, ErrorResponse> =
        settingsService.updateSettings(name, body)

    fun getSettings(name: String): Result<Settings, ErrorResponse> =
        settingsService.getSettings(name)

    fun getSchema(name: String): Result<JsonNode, ErrorResponse> =
        settingsService.getSchema(name)

    fun getSettingsClass(name: String): Result<Class<out Settings>, ErrorResponse> =
        settingsService.getClass(name)

    fun names(): Result<Collection<String>, ErrorResponse> =
        Result.ok(settingsService.names())

    fun shutdownProviders() =
        configurationService.shutdownProviders()

}
