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
