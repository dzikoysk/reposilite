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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.Checksum
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.ContentType.TEXT_XML
import panda.std.Result
import panda.std.asSuccess
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val MAX_MERGED_ENTRIES = 1024

/**
 * Serves maven-metadata.xml files (and their checksums) of repositories with enabled metadata merging
 * as a combination of the local file and the files provided by all mirrored repositories.
 */
internal class MergedMetadataService(
    private val journalist: Journalist,
    private val mirrorService: MirrorService,
    private val clock: Clock,
) : Journalist {

    private data class MergeKey(
        val repository: Repository,
        val gav: Location,
        /** Mirrors may be restricted to authenticated callers, so their results cannot be shared with anonymous ones. */
        val authenticated: Boolean,
    )

    private data class MergedMetadata(
        val content: ByteArray,
        val lastModifiedTime: Instant?,
    ) {
        private val checksums = ConcurrentHashMap<Checksum, ByteArray>()

        fun checksum(checksum: Checksum): ByteArray =
            checksums.computeIfAbsent(checksum) { it.generate(content.inputStream()).toByteArray() }
    }

    private class InFlightMerge(
        /** Loopback mirrors may lead back to the same key on the same thread, which must not wait for itself. */
        val owner: Thread,
        val result: CompletableFuture<Result<MergedMetadata, ErrorResponse>> = CompletableFuture(),
    )

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val merged: Cache<MergeKey, MergedMetadata> = Caffeine.newBuilder()
        .maximumSize(MAX_MERGED_ENTRIES.toLong())
        .ticker { TimeUnit.MILLISECONDS.toNanos(clock.millis()) }
        .expireAfter(object : Expiry<MergeKey, MergedMetadata> {
            override fun expireAfterCreate(key: MergeKey, value: MergedMetadata, currentTime: Long): Long =
                TimeUnit.SECONDS.toNanos(key.repository.metadataMaxAgeInSeconds.coerceAtLeast(0))

            override fun expireAfterUpdate(key: MergeKey, value: MergedMetadata, currentTime: Long, currentDuration: Long): Long =
                expireAfterCreate(key, value, currentTime)

            override fun expireAfterRead(key: MergeKey, value: MergedMetadata, currentTime: Long, currentDuration: Long): Long =
                currentDuration
        })
        .build()

    // Only cache bookkeeping is synchronized; fetching and waiting for mirrors happens outside this lock.
    private val cacheLock = Any()
    private val inFlightMerges = HashMap<MergeKey, InFlightMerge>()

    fun handles(repository: Repository, gav: Location): Boolean =
        repository.mergeMetadataEnabled && gav.toMetadataFile() != null

    fun findDetails(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<FileDetails, ErrorResponse> =
        findContent(repository, gav, accessToken).map { (content, lastModifiedTime) ->
            describe(gav, content, lastModifiedTime)
        }

    fun findFile(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<Pair<DocumentInfo, InputStream>, ErrorResponse> =
        findContent(repository, gav, accessToken).map { (content, lastModifiedTime) ->
            describe(gav, content, lastModifiedTime) to content.inputStream()
        }

    private fun describe(gav: Location, content: ByteArray, lastModifiedTime: Instant?): DocumentInfo =
        DocumentInfo(
            name = gav.getSimpleName(),
            contentType = gav.contentType(),
            contentLength = content.size.toLong(),
            lastModifiedTime = lastModifiedTime,
        )

    fun findInputStream(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<InputStream, ErrorResponse> =
        findContent(repository, gav, accessToken).map { (content, _) -> content.inputStream() }

    fun invalidate(gav: Location, recursive: Boolean = false) {
        // Other repositories may mirror the modified one through a loopback link, so their merged files are outdated as well
        val metadataFile = gav.toMetadataFile()
        if (metadataFile == null && !recursive) return

        val affected = { key: MergeKey -> key.gav == metadataFile || (recursive && key.gav.toString().startsWith("$gav/")) }
        synchronized(cacheLock) {
            // Running merges may have read the outdated state already, so later requests must start a new one
            inFlightMerges.keys.removeIf(affected)
            merged.asMap().keys.removeIf(affected)
        }
    }

    fun invalidateAll() {
        synchronized(cacheLock) {
            inFlightMerges.clear()
            merged.invalidateAll()
        }
    }

    private fun findContent(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<Pair<ByteArray, Instant?>, ErrorResponse> {
        val metadataFile = gav.toMetadataFile() ?: return notFoundError("Cannot merge '$gav'")

        return findMergedMetadata(repository, metadataFile, accessToken).map { metadata ->
            when (val checksum = gav.toChecksum()) {
                null -> metadata.content to metadata.lastModifiedTime
                else -> metadata.checksum(checksum) to metadata.lastModifiedTime
            }
        }
    }

    private fun findMergedMetadata(repository: Repository, metadataFile: Location, accessToken: AccessTokenIdentifier?): Result<MergedMetadata, ErrorResponse> {
        // Repository identity isolates cached results from settings reloads, including changed mirror permissions.
        val key = MergeKey(repository, metadataFile, accessToken != null)

        // Cache hits do not need the lock: a hit that races with an invalidation is equivalent to a read just before it.
        merged.getIfPresent(key)?.let { return it.asSuccess() }

        return when (val reservation = reserveMerge(key)) {
            is MergeReservation.Cached -> reservation.metadata.asSuccess()
            is MergeReservation.Join -> awaitMerge(reservation.running)
            is MergeReservation.Run -> runMerge(key, reservation, accessToken)
        }
    }

    private sealed interface MergeReservation {
        data class Cached(val metadata: MergedMetadata) : MergeReservation
        data class Join(val running: InFlightMerge) : MergeReservation
        data class Run(val merge: InFlightMerge) : MergeReservation
    }

    /**
     * Concurrent requests for the same file wait for a single merge instead of querying all mirrors again.
     * The merge runs on the calling thread, because it already distributes the mirror requests over ioService.
     */
    private fun reserveMerge(key: MergeKey): MergeReservation {
        val currentThread = Thread.currentThread()

        synchronized(cacheLock) {
            // Checked again, because another merge may have been published while waiting for the lock
            merged.getIfPresent(key)?.let { return MergeReservation.Cached(it) }

            val merge = InFlightMerge(currentThread)
            val running = inFlightMerges.putIfAbsent(key, merge)

            return when {
                running == null || running.owner == currentThread -> MergeReservation.Run(merge)
                else -> MergeReservation.Join(running)
            }
        }
    }

    private fun awaitMerge(running: InFlightMerge): Result<MergedMetadata, ErrorResponse> =
        try {
            running.result.join()
        } catch (exception: CompletionException) {
            // Waiting requests should fail like the request that ran the merge
            throw exception.cause ?: exception
        }

    private fun runMerge(key: MergeKey, reservation: MergeReservation.Run, accessToken: AccessTokenIdentifier?): Result<MergedMetadata, ErrorResponse> {
        val merge = reservation.merge

        return try {
            mergeMetadata(key.repository, key.gav, accessToken)
                .map { metadata -> serialize(metadata) }
                .peek { publish(key, it, merge) }
                .also { merge.result.complete(it) }
        } catch (throwable: Throwable) {
            merge.result.completeExceptionally(throwable)
            throw throwable
        } finally {
            synchronized(cacheLock) { inFlightMerges.remove(key, merge) }
        }
    }

    private fun serialize(metadata: Metadata): MergedMetadata =
        MergedMetadata(
            content = metadataXmlMapper.writeValueAsBytes(metadata),
            lastModifiedTime = metadata.versioning?.lastUpdated?.toInstant(),
        )

    private fun publish(key: MergeKey, metadata: MergedMetadata, merge: InFlightMerge) {
        synchronized(cacheLock) {
            // Invalidation removes affected reservations; unrelated writes must not prevent this merge from being cached.
            if (key.repository.metadataMaxAgeInSeconds > 0 && inFlightMerges[key] === merge) {
                merged.put(key, metadata)
            }
        }
    }

    private fun mergeMetadata(repository: Repository, metadataFile: Location, accessToken: AccessTokenIdentifier?): Result<Metadata, ErrorResponse> {
        val sources = mutableListOf<Metadata>()
        val failures = mutableListOf<ErrorResponse>()

        for ((source, result) in readLocalMetadata(repository, metadataFile) + fetchRemoteMetadata(repository, metadataFile, accessToken)) {
            result
                .onError { error -> if (error.status != 404) failures.add(error) }
                .peek { content -> parse(source, metadataFile, content)?.let { sources.add(it) } }
        }

        if (sources.isEmpty()) {
            return selectFailure(metadataFile, failures)
        }

        if (failures.isNotEmpty()) {
            logger.warn("MergedMetadata | Merged '$metadataFile' in ${repository.name} without ${failures.size} unavailable source(s): ${failures.joinToString { "${it.status}: ${it.message}" }}")
        }

        return MetadataMerger.merge(sources).asSuccess()
    }

    private fun readLocalMetadata(repository: Repository, metadataFile: Location): List<Pair<String, Result<ByteArray, ErrorResponse>>> =
        repository.metadataStorage.readLocalMetadata(metadataFile).fold(
            { contents -> contents.map { "local" to it.asSuccess() } },
            { error -> listOf("local" to Result.error(error)) },
        )

    private fun fetchRemoteMetadata(repository: Repository, metadataFile: Location, accessToken: AccessTokenIdentifier?): List<Pair<String, Result<ByteArray, ErrorResponse>>> =
        mirrorService
            .fetchFromAllRemoteHosts(repository, metadataFile, accessToken)
            .map { (host, result) -> host.host to result }

    /** Rate limits are preserved, so clients can distinguish them from missing files, as in the regular lookup. */
    private fun selectFailure(metadataFile: Location, failures: List<ErrorResponse>): Result<Metadata, ErrorResponse> =
        failures.firstOrNull { it.status == 429 }?.let { Result.error(it) }
            ?: failures.lastOrNull()?.let { Result.error(it) }
            ?: notFoundError("Cannot find '$metadataFile' in local or remote repositories")

    private fun parse(source: String, metadataFile: Location, content: ByteArray): Metadata? =
        runCatching { readMetadataXml(content) }
            .onFailure { logger.warn("MergedMetadata | Skipping malformed '$metadataFile' from $source: ${it.message}") }
            .getOrNull()

    private fun Location.toMetadataFile(): Location? =
        when (getSimpleName()) {
            METADATA_FILE -> this
            in checksumFileNames -> resolveSibling(METADATA_FILE)
            else -> null
        }

    private fun Location.toChecksum(): Checksum? =
        Checksum.entries.firstOrNull { getSimpleName() == "$METADATA_FILE.${it.extension}" }

    private fun Location.contentType(): ContentType =
        when (toChecksum()) {
            null -> TEXT_XML
            else -> ContentType.contentTypeByExtension(getExtension()) ?: APPLICATION_OCTET_STREAM
        }

    private fun String.toInstant(): Instant? =
        runCatching { LocalDateTime.parse(this, timestampFormatter).toInstant(ZoneOffset.UTC) }.getOrNull()

    override fun getLogger(): Logger =
        journalist.logger

    private companion object {
        val checksumFileNames = Checksum.entries.map { "$METADATA_FILE.${it.extension}" }.toSet()
    }

}
