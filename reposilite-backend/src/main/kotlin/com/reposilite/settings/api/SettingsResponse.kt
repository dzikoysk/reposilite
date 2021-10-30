package com.reposilite.settings.api

import io.javalin.http.ContentType

data class SettingsResponse(
    val type: ContentType,
    val content: String
)