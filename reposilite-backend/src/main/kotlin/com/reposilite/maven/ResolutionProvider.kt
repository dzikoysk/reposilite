/*
 * Copyright (c) 2026 dzikoysk
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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Result

internal class ResolutionProvider(
    private val journalist: Journalist,
) : Journalist {

    private data class ResolveAttempt<T>(
        val result: Result<T, ErrorResponse>,
        val remote: MirrorResolution<T>?,
    )

    fun <T : Any> resolve(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier?,
        mirrorFirst: Boolean,
        tryLocal: () -> Result<T, ErrorResponse>,
        tryRemote: (hosts: List<MirrorHost>) -> MirrorResolution<T>,
    ): Result<T, ErrorResponse> {
        val cache = repository.resolutionCache
        val authenticated = accessToken != null
        val cached = cache?.lookup(gav, authenticated)
        val isMetadata = gav.isResolutionMetadata()
        val notFoundMessage = "Cannot find '$gav' in local or remote repositories"

        if (isMetadata && cached == ResolutionCache.Origin.MissingMetadata) {
            return notFoundError(notFoundMessage)
        }

        val hosts = (cached as? ResolutionCache.Origin.Remote)
            ?.let { decision -> repository.mirrorHosts.filter { it.host == decision.host }.ifEmpty { repository.mirrorHosts } }
            ?: repository.mirrorHosts

        val attempt = when {
            cached is ResolutionCache.Origin.Remote || mirrorFirst -> tryRemoteFirst(tryLocal, tryRemote, hosts, notFoundMessage)
            else -> tryLocalFirst(tryLocal, tryRemote, hosts, notFoundMessage)
        }

        if (cache != null && isMetadata) {
            attempt.recordTo(cache, gav, authenticated)
        }

        return attempt.result
    }

    fun invalidate(repository: Repository, gav: Location): Boolean {
        if (gav.isResolutionMetadata()) {
            repository.resolutionCache?.invalidate(gav)
            return true
        }

        return false
    }

    private fun <T : Any> tryLocalFirst(
        tryLocal: () -> Result<T, ErrorResponse>,
        tryRemote: (List<MirrorHost>) -> MirrorResolution<T>,
        hosts: List<MirrorHost>,
        notFoundMessage: String,
    ): ResolveAttempt<T> =
        tryLocal().let { local ->
            when {
                local.isOk -> ResolveAttempt(local, remote = null)
                else -> {
                    val remote = tryRemote(hosts)
                    ResolveAttempt(remote.toResult(notFoundMessage).flatMapErr { local }, remote)
                }
            }
        }

    private fun <T : Any> tryRemoteFirst(
        tryLocal: () -> Result<T, ErrorResponse>,
        tryRemote: (List<MirrorHost>) -> MirrorResolution<T>,
        hosts: List<MirrorHost>,
        notFoundMessage: String,
    ): ResolveAttempt<T> =
        when (val remote = tryRemote(hosts)) {
            is MirrorResolution.Resolved -> ResolveAttempt(Result.ok(remote.value), remote)
            else -> ResolveAttempt(tryLocal().flatMapErr { remote.toResult(notFoundMessage) }, remote)
        }

    private fun <T> ResolveAttempt<T>.recordTo(cache: ResolutionCache, gav: Location, authenticated: Boolean) {
        val prefix = gav.getParent()
        if (prefix == Location.empty()) {
            return
        }

        // Successful resolve but upstream actually failed — we served a local fallback.
        // Log it for visibility, but do not cache: the upstream may recover.
        if (result.isOk && remote is MirrorResolution.Failed) {
            logger.warn("Resolution | Upstream failed (${remote.error.status}: ${remote.error.message}) — served '$gav' from local fallback")
            return
        }

        // Failed resolve: only NotFound is a stable signal worth caching. Everything else is transient.
        if (!result.isOk) {
            if (remote is MirrorResolution.NotFound) {
                cache.record(prefix, authenticated, ResolutionCache.Origin.MissingMetadata)
            }
            return
        }

        // Successful resolve: pin the host only when the mirror won't keep a local copy; otherwise we served locally.
        val origin = when (remote) {
            is MirrorResolution.Resolved if !remote.mirror.configuration.store -> ResolutionCache.Origin.Remote(remote.mirror.host)
            else -> ResolutionCache.Origin.Local
        }

        cache.record(prefix, authenticated, origin)
    }

    private fun Location.isResolutionMetadata(): Boolean =
        getSimpleName().let { name ->
            when {
                name.endsWith(".pom") -> true
                name.endsWith(".module") -> true
                name == METADATA_FILE -> true
                name.endsWith(".xml") -> name == "ivy.xml" || name.startsWith("ivy-")
                else -> false
            }
        }

    override fun getLogger(): Logger =
        journalist.logger

}
