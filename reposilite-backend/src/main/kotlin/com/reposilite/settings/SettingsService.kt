package com.reposilite.settings

import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.Settings
import com.reposilite.settings.api.SettingsHandler
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.MutableReference

class SettingsService(
    private val schemaGenerator: SchemaGenerator
) {

    private val configHandlers = mutableMapOf<String, SettingsHandler<*>>()

    fun <T : Settings> registerSettingsWatcher(handler: SettingsHandler<T>): SettingsHandler<T> = handler.also {
        if (it.name in configHandlers) throw IllegalArgumentException("There are already settings with that name! Please report to the plugin author.")
        configHandlers[it.name] = it
    }

    fun <T : Settings> registerSettingsWatcher(type: Class<T>, getter: () -> T, setter: (T) -> Unit): SettingsHandler<T> =
        registerSettingsWatcher(object : SettingsHandler<T> {
            override val name = type.getAnnotation(Doc::class.java).title.sanitizeURLParam()
            override val type = type
            override val schema = schemaGenerator.generateSchema(type).also { cleanupScheme(it)}

            override fun get(): T = getter()
            override fun update(value: T) = setter(value)

            private fun String.sanitizeURLParam(): String = lowercase().replace(' ', '_')
        })

    fun <T : Settings> registerSettingsWatcher(type: Class<T>, reference: MutableReference<T>): SettingsHandler<T> =
        registerSettingsWatcher(type, reference::get) { reference.update(it) }

    fun getClass(name: String): Result<Class<out Settings>, ErrorResponse> =
        getHandler(name).map { it.type }

    fun getSettings(name: String): Result<Settings, ErrorResponse> =
        getHandler(name).map { it.get() }

    @Suppress("UNCHECKED_CAST")
    fun <S : Settings> updateSettings(name: String, body: S): Result<S, ErrorResponse> =
        getHandler(name)
            .map { it as SettingsHandler<S> }
            .flatMap { it.updateHandler(body) }

    @Suppress("UNCHECKED_CAST")
    private fun <S : Settings> SettingsHandler<S>.updateHandler(value: S): Result<S, ErrorResponse> =
        Result.attempt { update(value) }
            .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, it.message.orEmpty()) }
            .map { get() }

    fun getSchema(name: String): Result<JsonNode, ErrorResponse> =
        getHandler(name).map { it.schema }

    private fun getHandler(name: String): Result<SettingsHandler<*>, ErrorResponse> =
        configHandlers[name]
            ?.asSuccess()
            ?: notFoundError("No configuration with name '$name' found")

    fun names(): Collection<String> =
        configHandlers.keys

}
