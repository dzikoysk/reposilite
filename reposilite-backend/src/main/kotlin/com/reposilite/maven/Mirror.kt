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

package com.reposilite.maven

import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.http.RemoteClient
import com.reposilite.shared.notFoundError
import panda.std.Result

data class MirrorHost(
    val host: String,
    val configuration: MirroredRepositorySettings,
    val client: RemoteClient
)

enum class StoragePolicy {
    PRIORITIZE_UPSTREAM_METADATA,
    STRICT
}

internal sealed interface MirrorResolution<out T> {
    data object NoEligibleHosts : MirrorResolution<Nothing>
    data object NotFound : MirrorResolution<Nothing>
    data class Failed(val error: ErrorResponse) : MirrorResolution<Nothing>
    data class Resolved<T>(val value: T, val mirror: MirrorHost) : MirrorResolution<T>
}

internal fun <T : Any> MirrorResolution<T>.toResult(notFoundMessage: String): Result<T, ErrorResponse> =
    when (this) {
        is MirrorResolution.Resolved -> Result.ok(value)
        is MirrorResolution.Failed -> Result.error(error)
        MirrorResolution.NoEligibleHosts, MirrorResolution.NotFound -> notFoundError(notFoundMessage)
    }
