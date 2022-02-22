package com.reposilite.settings

import com.reposilite.settings.api.SettingsHandler
import com.reposilite.plugin.api.Facade
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode
import panda.std.Result

class SharedConfigurationFacade internal constructor(): Facade {
    private val configHandlers = mutableMapOf<String, SettingsHandler<*>>()

    fun registerHandler(handler: SettingsHandler<*>) = configHandlers.put(handler.name, handler)

    fun getClassForName(name: String): Result<Class<*>, ErrorResponse> = getHandler(name).map { it.type }

    fun getConfiguration(name: String): Result<Any, ErrorResponse> {
        return getHandler(name).map { configHandlers[name]!!.get()!! }
    }

    fun updateConfiguration(name: String, body: Any): Result<Any, ErrorResponse> {
        return getHandler(name).flatMap {
            Result.attempt { configHandlers[name]!!.update(body)!! }
                .mapErr { ErrorResponse(HttpCode.INTERNAL_SERVER_ERROR, it.message.orEmpty()) }
        }
    }

    private fun getHandler(name: String): Result<SettingsHandler<*>, ErrorResponse> {
        return Result.`when`(configHandlers.containsKey(name),
            { configHandlers[name]!! },
            { ErrorResponse(HttpCode.NOT_FOUND, "No configuration with name \"$name\" found!") }
        )
    }
}

private fun <T> SettingsHandler<T>.update(value: Any): Any? {
    return this.update(value as T)
}
