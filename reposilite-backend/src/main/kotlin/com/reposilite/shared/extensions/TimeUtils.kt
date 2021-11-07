/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.shared.extensions

import java.util.Locale

internal object TimeUtils {

    fun getUptimeInSeconds(uptime: Long): Double =
        getUptime(uptime) / 1000.0

    fun getUptime(startTime: Long): Long =
        System.currentTimeMillis() - startTime

    fun getPrettyUptimeInSeconds(startTime: Long): String =
        format(getUptimeInSeconds(startTime)) + "s"

    fun getPrettyUptimeInMinutes(startTime: Long): String =
        format(getUptimeInSeconds(startTime) / 60) + "min"

    fun format(time: Double): String =
        String.format(Locale.US, "%.2f", time)

}