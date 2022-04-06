package com.reposilite.settings.api

import com.fasterxml.jackson.databind.JsonNode

interface SettingsHandler<T> {

    val name: String
    val type: Class<T>
    val schema: JsonNode

    fun get(): T

    fun update(value: T)

}
