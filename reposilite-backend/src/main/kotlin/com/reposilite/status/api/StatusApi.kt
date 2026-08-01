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

package com.reposilite.status.api

import com.reposilite.statistics.toUTCMillis
import java.time.LocalDateTime

data class InstanceStatusResponse(
    val version: String,
    val latestVersion: String,
    val uptime: Long,
    val usedMemory: Double,
    val maxMemory: Int,
    val usedThreads: Int,
    val maxThreads: Int,
    val failuresCount: Int,
    val sponsors: List<List<String>>
)

data class RecordedFailure(
    val path: String,
    val type: String,
    val message: String,
    val messages: List<String>,
    val trace: String,
    val occurrences: Int
)

data class FailuresResponse(
    val failures: List<RecordedFailure>
)

data class StatusSnapshot(
    val at: Long = LocalDateTime.now().toUTCMillis(),
    val memory: Int,
    val threads: Int
)

data class HealthResponse(
    val status: String
)
