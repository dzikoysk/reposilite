package com.reposilite.settings.api

import com.fasterxml.jackson.databind.JsonNode
import java.io.Serializable

interface Settings : Serializable

interface SettingsHandler<S : Settings> {
    val name: String
    val type: Class<S>
    val schema: JsonNode

    fun get(): S
    fun update(value: S)
}
