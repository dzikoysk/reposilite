package com.reposilite.settings

import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.SettingsHandler
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.MutableReference
import java.util.function.Consumer
import java.util.function.Supplier

class SettingsService(
    private val schemaGenerator: SchemaGenerator
) {

    private val configHandlers = mutableMapOf<String, SettingsHandler<*>>()

    fun <T> registerSettingsWatcher(handler: SettingsHandler<T>): SettingsHandler<T> = handler.also {
        if (it.name in configHandlers) throw IllegalArgumentException("There are already settings with that name! Please report to the plugin author.")
        configHandlers[it.name] = it
    }

    fun <T> registerSettingsWatcher(type: Class<T>, getter: Supplier<T>, setter: Consumer<T>): SettingsHandler<T> =
        registerSettingsWatcher(object : SettingsHandler<T> {
            override val name = type.getAnnotation(Doc::class.java).title.sanitizeURLParam()
            override val type = type
            override val schema = schemaGenerator.generateSchema(type)
            override fun get(): T = getter.get()
            override fun update(value: T) = setter.accept(value)
        })

    @Suppress("MoveLambdaOutsideParentheses")
    fun <T> registerSettingsWatcher(type: Class<T>, reference: MutableReference<T>): SettingsHandler<T> =
        registerSettingsWatcher(type, reference::get, { reference.update(it) })

    fun getClass(name: String): Result<Class<*>, ErrorResponse> =
        getHandler(name).map { it.type }

    fun getSettings(name: String): Result<Any, ErrorResponse> =
        getHandler(name).map { configHandlers[name]!!.get()!! }

    fun updateSettings(name: String, body: Any): Result<Any, ErrorResponse> =
        getHandler(name).flatMap {
            Result.attempt { configHandlers[name]!!.update(body)!! }
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, it.message.orEmpty()) }
        }

    fun getSchema(name: String): Result<JsonNode, ErrorResponse> =
        getHandler(name).map { it.schema }

    private fun getHandler(name: String): Result<SettingsHandler<*>, ErrorResponse> =
        configHandlers[name]
            ?.asSuccess()
            ?: notFoundError("No configuration with name '$name' found")

    @Suppress("UNCHECKED_CAST")
    private fun <T> SettingsHandler<T>.update(value: Any): Any? {
        this.update(value as T)
        return this.get()
    }

    fun listSettings(): Map<String, String> =
        configHandlers.mapValues { it.value.type.getAnnotation(Doc::class.java).title }

}

private fun String.sanitizeURLParam(): String {
    return lowercase().replace(' ', '_')
}
