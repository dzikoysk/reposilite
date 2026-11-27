/*
 * Copyright (c) 2020-2026 dzikoysk
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
import com.reposilite.shared.internalServerError
import com.reposilite.shared.notFoundError
import com.reposilite.shared.toErrorResult
import com.reposilite.storage.api.Location
import io.javalin.http.HttpStatus.BAD_GATEWAY
import panda.std.Result

data class MirrorHost(
    val host: String,
    val configuration: MirroredRepositorySettings,
    val client: RemoteClient
) {
    internal fun head(gav: Location) =
        client.head(uriFor(gav), configuration.authorization, configuration.connectTimeout, configuration.readTimeout)

    internal fun get(gav: Location) =
        client.get(uriFor(gav), configuration.authorization, configuration.connectTimeout, configuration.readTimeout)
            .mapErr { error -> error.updateMessage { "$host: $it" } }

    private fun uriFor(gav: Location): String {
        if (client is RepositoryLoopbackClient) {
            return "$host/$gav"
        }

        val base = host.substringBefore('?').substringBefore('#')
        return "${base.removeSuffix("/")}/${gav.toUrlPath()}${host.removePrefix(base)}"
    }
}

enum class StoragePolicy {
    PRIORITIZE_UPSTREAM_METADATA,
    STRICT
}

internal sealed interface MirrorResolution<out T> {
    data object NoEligibleHosts : MirrorResolution<Nothing>
    data object NotFound : MirrorResolution<Nothing>
    data class Failed(
        val error: ErrorResponse,
        val source: MirrorFailureSource = MirrorFailureSource.UPSTREAM,
    ) : MirrorResolution<Nothing>
    data class Resolved<T>(val value: T, val mirror: MirrorHost) : MirrorResolution<T>
}

internal enum class MirrorFailureSource {
    LOCAL,
    UPSTREAM,
}

internal fun <T : Any> MirrorResolution<T>.toResult(gav: Location): Result<T, ErrorResponse> =
    when (this) {
        is MirrorResolution.Resolved -> Result.ok(value)
        is MirrorResolution.Failed -> when {
            source == MirrorFailureSource.LOCAL && error.status == 404 -> internalServerError("Local storage failed")
            source == MirrorFailureSource.LOCAL -> Result.error(error)
            error.status == 429 -> Result.error(error)
            else -> BAD_GATEWAY.toErrorResult("Upstream repository failed")
        }
        MirrorResolution.NoEligibleHosts, MirrorResolution.NotFound -> notFoundError("Cannot find '$gav' in local or remote repositories")
    }
