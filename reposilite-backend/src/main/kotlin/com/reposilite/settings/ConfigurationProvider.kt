package com.reposilite.settings

import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.util.concurrent.ScheduledExecutorService

interface ConfigurationProvider<T> {

    val name: String
    val displayName: String
    val configuration: T

    fun initialize()

    fun registerWatcher(scheduler: ScheduledExecutorService)

    fun resolve(configurationName: String): Result<SettingsResponse, ErrorResponse>

    fun update(request: SettingsUpdateRequest): Result<Unit, ErrorResponse>

    fun shutdown()

}