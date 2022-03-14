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

package com.reposilite.status

import com.reposilite.VERSION
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.extensions.TimeUtils
import panda.utilities.IOUtils
import panda.utilities.console.Effect.*

class StatusFacade(
    private val testEnv: Boolean,
    val startTime: Long = System.currentTimeMillis(),
    private val status: () -> Boolean,
    private val remoteVersionUrl: String
) : Facade {

    fun isAlive(): Boolean =
        status()

    internal fun uptime(): String =
        TimeUtils.getPrettyUptimeInMinutes(startTime)

    internal fun memoryUsage(): String =
        TimeUtils.format((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0) + "M"

    internal fun threadGroupUsage(): String =
        Thread.activeCount().toString()

    internal fun getLatestVersion(): String =
        if (testEnv)
            "<unknown>"
        else
            IOUtils.fetchContent(remoteVersionUrl).fold(
                { "${if (VERSION == it) GREEN else RED_UNDERLINED}$it$RESET" },
                { when (it.message?.contains("java.security.NoSuchAlgorithmException")) {
                    true -> "Cannot load SSL context for HTTPS request due to the lack of available memory"
                    else -> "$remoteVersionUrl is unavailable: ${it.message}"
                } }
            )

}
