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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.StoragePolicy.PRIORITIZE_UPSTREAM_METADATA
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.internalServer
import com.reposilite.shared.notFound
import com.reposilite.status.FailureFacade
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Blank
import panda.std.Result
import panda.std.Result.error
import panda.std.Result.ok
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

internal const val NO_EXTENSION_MARKER = "<none>"

internal class MirrorService(
    private val journalist: Journalist,
    private val failureFacade: FailureFacade,
    private val clock: Clock,
    private val ioService: ExecutorService,
) : Journalist {

    private data class FetchKey(
        val repository: String,
        val gav: Location,
        /**
         * Authenticated and anonymous callers are bucketed separately,
         * because a host configured `authenticatedFetchingOnly` is only reachable for authenticated callers.
         * Collapsing the two would let an anonymous "not found" poison the dedup result for a privileged caller.
         */
        val authenticated: Boolean,
    )

    private val inFlightFetches = ConcurrentHashMap<FetchKey, CompletableFuture<MirrorResolution<Unit>>>()

    fun shouldPrioritizeMirrorRepository(repository: Repository, gav: Location): Boolean =
        when {
            repository.storagePolicy == PRIORITIZE_UPSTREAM_METADATA && gav.getSimpleName().contains(METADATA_FILE) ->
                repository.metadataMaxAgeInSeconds <= 0 || !isMetadataFileValid(repository, gav)
            else ->
                false
        }

    private fun isMetadataFileValid(repository: Repository, gav: Location): Boolean =
        repository.storageProvider.getLastModifiedTime(gav)
            .map { it.toInstant().plus(repository.metadataMaxAgeInSeconds, ChronoUnit.SECONDS) }
            .matches { it.isAfter(Instant.now(clock)) }

    fun findRemoteDetails(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier? = null,
        hosts: List<MirrorHost> = repository.mirrorHosts,
    ): MirrorResolution<FileDetails> =
        searchInRemoteRepositories(repository, gav, accessToken, hosts) { (host, config, client) ->
            client.head("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
        }

    fun findRemoteFile(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier? = null,
        hosts: List<MirrorHost> = repository.mirrorHosts,
    ): MirrorResolution<InputStream> {
        // Single-flight only kicks in when every host stores; if any host streams without persisting,
        // waiters can't share the result and we fall back to per-caller fetches.
        if (hosts.isEmpty() || !hosts.all { it.configuration.store }) {
            return fetchFromRemoteHosts(repository, gav, accessToken, hosts)
        }

        val key = FetchKey(
            repository = repository.name,
            gav = gav,
            authenticated = accessToken != null,
        )

        val future = inFlightFetches.computeIfAbsent(key) {
            try {
                CompletableFuture.supplyAsync({
                    try {
                        fetchAndStoreFromRemoteHosts(repository, gav, accessToken, hosts)
                    } catch (throwable: Throwable) {
                        failureFacade.throwException("Mirror fetch ${repository.name}/$gav", throwable)
                        MirrorResolution.Failed(internalServer("Mirror fetch failed: ${throwable.message}"))
                    }
                }, ioService)
            } catch (rejected: RejectedExecutionException) {
                // ioService refused the task (typically: shutdown is in progress). Surface as a normal
                // ErrorResponse rather than letting the runtime exception escape into Javalin.
                CompletableFuture.completedFuture(MirrorResolution.Failed(internalServer("Mirror fetch rejected: ${rejected.message}")))
            }
        }

        return future
            .whenComplete { _, _ -> inFlightFetches.remove(key, future) }
            .join().openLocal(repository, gav)
    }

    private fun MirrorResolution<Unit>.openLocal(repository: Repository, gav: Location): MirrorResolution<InputStream> =
        when (this) {
            is MirrorResolution.Resolved -> repository.storageProvider.getFile(gav).fold({ MirrorResolution.Resolved(it, mirror) }, { MirrorResolution.Failed(it) })
            is MirrorResolution.Failed -> this
            MirrorResolution.NotFound -> MirrorResolution.NotFound
            MirrorResolution.NoEligibleHosts -> MirrorResolution.NoEligibleHosts
        }

    private fun fetchFromRemoteHosts(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?, hosts: List<MirrorHost>): MirrorResolution<InputStream> =
        searchInRemoteRepositories(repository, gav, accessToken, hosts) { (host, config, client) ->
            client
                .get("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
                .flatMap { data ->
                    when {
                        config.store -> storeFile(repository, gav, data)
                        else -> ok(data)
                    }
                }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun fetchAndStoreFromRemoteHosts(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?, hosts: List<MirrorHost>): MirrorResolution<Unit> =
        searchInRemoteRepositories(repository, gav, accessToken, hosts) { (host, config, client) ->
            client
                .get("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
                .flatMap { data -> repository.storageProvider.putFile(gav, data) }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun storeFile(repository: Repository, gav: Location, data: InputStream): Result<InputStream, ErrorResponse> =
        repository
            .storageProvider
            .putFile(gav, data)
            .flatMap { repository.storageProvider.getFile(gav) }

    private fun <V> searchInRemoteRepositories(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier?,
        hosts: List<MirrorHost>,
        fetch: (MirrorHost) -> Result<V, ErrorResponse>,
    ): MirrorResolution<V> {
        val eligibleHosts = hosts
            .filter { !it.configuration.authenticatedFetchingOnly || accessToken != null }
            .filter { (_, config) ->
                isAllowed(config, gav).fold(
                    { true },
                    { reason ->
                        logger.debug("MirrorService | Cannot request '$gav' from remote repository '${repository.name}' (reason: illegal $reason)")
                        false
                    }
                )
            }
        if (eligibleHosts.isEmpty()) {
            return MirrorResolution.NoEligibleHosts
        }

        var allMissing = true
        var lastUpstreamError: ErrorResponse? = null
        for (host in eligibleHosts) {
            val result = fetch(host)
            if (result.isOk) {
                return MirrorResolution.Resolved(result.get(), host)
            }
            if (result.error.status != 404) {
                allMissing = false
                lastUpstreamError = result.error
            }
        }
        return when {
            allMissing -> MirrorResolution.NotFound
            else -> MirrorResolution.Failed(lastUpstreamError ?: notFound("Cannot find '$gav' in remote repositories"))
        }
    }

    private enum class DisallowedReason {
        EXTENSION,
        GROUP
    }

    private fun isAllowed(config: MirroredRepositorySettings, gav: Location): Result<Blank, DisallowedReason> =
        isAllowedExtension(config, gav).flatMap { isAllowedGroup(config, gav) }

    private fun isAllowedExtension(config: MirroredRepositorySettings, gav: Location): Result<Blank, DisallowedReason> =
        when {
            config.allowedExtensions.none { it.isNotBlank() } -> ok()
            config.allowedExtensions.any { matchesExtension(it, gav) } -> ok()
            else -> error(DisallowedReason.EXTENSION)
        }

    private fun matchesExtension(entry: String, gav: Location): Boolean =
        when (val trimmed = entry.trim()) {
            NO_EXTENSION_MARKER -> gav.getExtension().isEmpty()
            else -> gav.endsWith(trimmed)
        }

    private fun isAllowedGroup(config: MirroredRepositorySettings, gav: Location): Result<Blank, DisallowedReason> =
        when {
            config.allowedGroups.none { it.isNotBlank() } -> ok()
            config.allowedGroups.any { gav.toString().startsWith(it.replace('.', '/')) } -> ok()
            else -> error(DisallowedReason.GROUP)
        }

    override fun getLogger(): Logger =
        journalist.logger

}
