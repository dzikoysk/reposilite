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

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.Facade
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.SchemaHandler
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.settings.infrastructure.FileSystemConfigurationProvider
import com.reposilite.settings.infrastructure.SqlConfigurationProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.HttpCode
import org.jetbrains.exposed.sql.Database
import panda.std.Result
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer
import java.util.function.Supplier

class SettingsFacade internal constructor(
    private val journalist: Journalist,
    private val workingDirectory: Path,
    val localConfiguration: LocalConfiguration,
    val database: Lazy<Database>,
    private val settingsRepository: SettingsRepository,
    private val schemaGenerator: SchemaGenerator
) : Facade {

    private val configurationProviders = mutableMapOf<String, ConfigurationProvider<*>>()
    private val configHandlers = mutableMapOf<String, SchemaHandler<*>>()

    fun <T> registerSchemaWatcher(type: Class<T>, getter: Supplier<T>, updater: Consumer<T>): SchemaHandler<T> =
        object : SchemaHandler<T> {
            override val name = type.getAnnotation(Doc::class.java).title
            override val type = type
            override val schema = schemaGenerator.generateSchema(type)
            override fun get(): T = getter.get()
            override fun update(value: T): Unit = updater.accept(value)
        }.also {
            configHandlers[it.name] = it
        }

    fun getClassForName(name: String): Result<Class<*>, ErrorResponse> =
        getHandler(name).map { it.type }

    fun getConfiguration(name: String): Result<Any, ErrorResponse> =
        getHandler(name).map { configHandlers[name]!!.get()!! }

    fun updateConfiguration(name: String, body: Any): Result<Any, ErrorResponse> =
        getHandler(name).flatMap {
            Result.attempt { configHandlers[name]!!.update(body)!! }
                .mapErr { ErrorResponse(HttpCode.INTERNAL_SERVER_ERROR, it.message.orEmpty()) }
        }

    fun getHandler(name: String): Result<SchemaHandler<*>, ErrorResponse> =
        Result.`when`(configHandlers.containsKey(name),
            { configHandlers[name]!! },
            { ErrorResponse(HttpCode.NOT_FOUND, "No configuration with name \"$name\" found!") }
        )

    val sharedConfiguration: SharedConfiguration // expose it directly for easier calls
        get() = findConfiguration()

    fun <C : Any> createConfigurationProvider(configuration: C, displayName: String, name: String, mode: String = "none", configurationFile: Path? = null): ConfigurationProvider<C> =
        registerCustomConfigurationProvider(
            if (mode == "none")
                SqlConfigurationProvider(
                    name = name,
                    displayName = displayName,
                    journalist = journalist,
                    settingsRepository = settingsRepository,
                    configuration = configuration
                )
            else
                FileSystemConfigurationProvider(
                    name = name,
                    displayName = displayName,
                    journalist = journalist,
                    workingDirectory = workingDirectory,
                    configurationFile = configurationFile ?: workingDirectory.resolve(name),
                    mode = mode,
                    configuration = configuration,
                )
        )

    fun <C : Any> registerCustomConfigurationProvider(configurationProvider: ConfigurationProvider<C>): ConfigurationProvider<C> =
        configurationProvider.also {
            configurationProviders[it.name] = it
            it.initialize()
        }

    internal fun attachWatcherScheduler(scheduler: ScheduledExecutorService) =
        configurationProviders.forEach { (_, provider) -> provider.registerWatcher(scheduler) }

    fun resolveConfiguration(name: String): Result<SettingsResponse, ErrorResponse> =
        configurationProviders[name]?.resolve(name) ?: notFoundError("Configuration $name not found")

    fun updateConfiguration(request: SettingsUpdateRequest): Result<Unit, ErrorResponse> =
        configurationProviders[request.name]?.update(request) ?: notFoundError("Configuration ${request.name} not found")

    fun shutdownProviders() =
        configurationProviders.forEach { (_, provider) -> provider.shutdown() }

    @Suppress("UNCHECKED_CAST")
    fun <C : Any> findConfiguration(type: Class<C>): C =
        configurationProviders.values
            .first { type.isInstance(it.configuration) }
            .configuration as C

    inline fun <reified C : Any> findConfiguration(): C =
        findConfiguration(C::class.java)

}

@Suppress("UNCHECKED_CAST")
private fun <T> SchemaHandler<T>.update(value: Any): Any? {
    this.update(value as T)
    return this.get()
}
