package com.reposilite.settings

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.settings.api.Doc
import com.reposilite.settings.api.SchemaHandler
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.MutableReference

class SchemaService(
    private val schemaGenerator: SchemaGenerator
) {

    private val configHandlers = mutableMapOf<String, SchemaHandler<*>>()

    fun <T> registerSchemaWatcher(type: Class<T>, reference: MutableReference<T>): SchemaHandler<T> =
        object : SchemaHandler<T> {
            override val name = type.getAnnotation(Doc::class.java).title
            override val type = type
            override val schema = schemaGenerator.generateSchema(type)
            override fun get(): T = reference.get()
            override fun update(value: T) { reference.update(value) }
        }.also {
            configHandlers[it.name] = it
        }

    fun getSettingsClassForName(name: String): Result<Class<*>, ErrorResponse> =
        getHandler(name).map { it.type }

    fun getConfiguration(name: String): Result<Any, ErrorResponse> =
        getHandler(name).map { configHandlers[name]!!.get()!! }

    fun updateConfiguration(name: String, body: Any): Result<Any, ErrorResponse> =
        getHandler(name).flatMap {
            Result.attempt { configHandlers[name]!!.update(body)!! }
                .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, it.message.orEmpty()) }
        }

    fun getHandler(name: String): Result<SchemaHandler<*>, ErrorResponse> =
        configHandlers[name]
            ?.asSuccess()
            ?: notFoundError("No configuration with name '$name' found")

    @Suppress("UNCHECKED_CAST")
    private fun <T> SchemaHandler<T>.update(value: Any): Any? {
        this.update(value as T)
        return this.get()
    }

}