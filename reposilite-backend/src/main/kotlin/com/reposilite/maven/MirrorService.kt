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
import com.reposilite.shared.notFoundError
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

internal const val NO_EXTENSION_MARKER = "<none>"

internal class MirrorService(
    private val journalist: Journalist,
    private val clock: Clock
) : Journalist {

    // Tracks an in-flight upstream fetch+store per (repository, gav) so that N concurrent client
    // requests for the same artifact collapse into a single upstream call. Each waiter then opens
    // its own fresh InputStream from local storage; the future itself only carries success/error.
    private val inFlightFetches = ConcurrentHashMap<Pair<String, Location>, CompletableFuture<Result<Unit, ErrorResponse>>>()

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

    fun findRemoteDetails(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier? = null): Result<FileDetails, ErrorResponse> =
        searchInRemoteRepositories(repository, gav, accessToken) { (host, config, client) ->
            client.head("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
        }

    fun findRemoteFile(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier? = null): Result<InputStream, ErrorResponse> {
        // Single-flight only applies when every mirror host stores; otherwise we'd have to coordinate
        // a fetch path that doesn't write to local storage, and waiters can't share a remote stream.
        if (repository.mirrorHosts.isEmpty() || !repository.mirrorHosts.all { it.configuration.store }) {
            return fetchFromRemoteHosts(repository, gav, accessToken)
        }

        val key = repository.name to gav
        val future = inFlightFetches.computeIfAbsent(key) {
            CompletableFuture.supplyAsync { fetchAndStoreFromRemoteHosts(repository, gav, accessToken) }
                .also { it.whenComplete { _, _ -> inFlightFetches.remove(key, it) } }
        }

        return future.join().flatMap { repository.storageProvider.getFile(gav) }
    }

    private fun fetchFromRemoteHosts(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<InputStream, ErrorResponse> =
        searchInRemoteRepositories(repository, gav, accessToken) { (host, config, client) ->
            client.get("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
                .flatMap { data -> if (config.store) storeFile(repository, gav, data) else ok(data) }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun fetchAndStoreFromRemoteHosts(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<Unit, ErrorResponse> =
        searchInRemoteRepositories(repository, gav, accessToken) { (host, config, client) ->
            client.get("${host.removeSuffix("/")}/$gav", config.authorization, config.connectTimeout, config.readTimeout)
                .flatMap { data -> repository.storageProvider.putFile(gav, data) }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun storeFile(repository: Repository, gav: Location, data: InputStream): Result<InputStream, ErrorResponse> =
        repository.storageProvider
            .putFile(gav, data)
            .flatMap { repository.storageProvider.getFile(gav) }

    private fun <V> searchInRemoteRepositories(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?, fetch: (MirrorHost) -> Result<V, ErrorResponse>): Result<V, ErrorResponse> =
        repository.mirrorHosts.asSequence()
            .filter { !it.configuration.authenticatedFetchingOnly || accessToken != null  }
            .filter { (_, config) ->
                isAllowed(config, gav).fold(
                    { true },
                    { reason ->
                        logger.debug("MirrorService | Cannot request '$gav' from remote repository '${repository.name}' (reason: illegal $reason)")
                        false
                    }
                )
            }
            .map { fetch(it) }
            .firstOrNull { it.isOk }
            ?: notFoundError("Cannot find '$gav' in remote repositories")

    private enum class DisallowedReason {
        EXTENSION,
        GROUP
    }

    private fun isAllowed(config: MirroredRepositorySettings, gav: Location): Result<Blank, DisallowedReason> =
        isAllowedExtension(config, gav).flatMap { isAllowedGroup(config, gav) }

    private fun isAllowedExtension(config: MirroredRepositorySettings, gav: Location): Result<Blank, DisallowedReason> =
        when {
            config.allowedExtensions.none { it.isNotBlank() } -> ok()
            config.allowedExtensions.any { entry ->
                when (val trimmed = entry.trim()) {
                    NO_EXTENSION_MARKER -> gav.getExtension().isEmpty()
                    else -> gav.endsWith(trimmed)
                }
            } -> ok()
            else -> error(DisallowedReason.EXTENSION)
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
