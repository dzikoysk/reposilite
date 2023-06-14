/*
 * Copyright (c) 2023 dzikoysk
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

import java.time.Duration
import java.time.Instant
import java.util.Locale

internal object TimeUtils {

    private fun getUptimeInSeconds(uptime: Long): Double =
        getUptime(uptime) / 1000.0

    private fun getUptime(startTime: Long): Long =
        System.currentTimeMillis() - startTime

    fun getPrettyUptimeInSeconds(startTime: Long): String =
        format(getUptimeInSeconds(startTime)) + "s"

    fun getPrettyUptime(startTime: Long): String {
        val currentTimestamp = Instant.now().toEpochMilli()
        val uptimeMillis = currentTimestamp - startTime

        val uptimeDuration = Duration.ofMillis(uptimeMillis)
        val days = uptimeDuration.toDays()
        val hours = uptimeDuration.toHours() % 24
        val minutes = uptimeDuration.toMinutes() % 60
        val seconds = uptimeDuration.seconds % 60

        val uptimeStringBuilder = StringBuilder()

        if (days > 0) {
            uptimeStringBuilder.append(days).append("d ")
        }

        if (hours > 0) {
            uptimeStringBuilder.append(hours).append("h ")
        }

        if (minutes > 0) {
            uptimeStringBuilder.append(minutes).append("min ")
        }

        if (seconds > 0 || days == 0L && hours == 0L && minutes == 0L) {
            uptimeStringBuilder.append(seconds).append("s")
        }

        return uptimeStringBuilder.toString().trim()
    }

    fun format(time: Double): String =
        String.format(Locale.US, "%.2f", time)

}
