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

import com.fasterxml.jackson.module.kotlin.readValue
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
    )

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

    /** Incremented by every invalidation, so merges that started before it do not populate the cache. */
    private var generation = 0L

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
        val affected = { key: MergeKey -> key.gav == metadataFile || (recursive && key.gav.toString().startsWith("$gav/")) }
        synchronized(cacheLock) {
            generation++
            // Running merges may have read the outdated state already, so later requests must start a new one
            inFlightMerges.keys.removeIf(affected)
            merged.asMap().keys.removeIf(affected)
        }
    }

    fun invalidateAll() {
        synchronized(cacheLock) {
            generation++
            inFlightMerges.clear()
            merged.invalidateAll()
        }
    }

    private fun findContent(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier?): Result<Pair<ByteArray, Instant?>, ErrorResponse> {
        val metadataFile = gav.toMetadataFile() ?: return notFoundError("Cannot merge '$gav'")

        return findMergedMetadata(repository, metadataFile, accessToken).map { metadata ->
            when (val checksum = gav.toChecksum()) {
                null -> metadata.content to metadata.lastModifiedTime
                else -> checksum.generate(metadata.content.inputStream()).toByteArray() to metadata.lastModifiedTime
            }
        }
    }

    private fun findMergedMetadata(repository: Repository, metadataFile: Location, accessToken: AccessTokenIdentifier?): Result<MergedMetadata, ErrorResponse> {
        // Repository identity isolates cached results from settings reloads, including changed mirror permissions.
        val key = MergeKey(repository, metadataFile, accessToken != null)

        // Cache hits do not need the lock: a hit that races with an invalidation is equivalent to a read just before it.
        merged.getIfPresent(key)?.let { return it.asSuccess() }

        // Concurrent requests for the same file wait for a single merge instead of querying all mirrors again.
        // The merge runs on the calling thread, because it already distributes the mirror requests over ioService.
        val merge = InFlightMerge(Thread.currentThread())
        val (running, startedAt) = synchronized(cacheLock) {
            // Checked again, because another merge may have been published while waiting for the lock
            merged.getIfPresent(key)?.let { return it.asSuccess() }
            inFlightMerges.putIfAbsent(key, merge) to generation
        }

        if (running != null && running.owner != Thread.currentThread()) {
            return try {
                running.result.join()
            } catch (exception: CompletionException) {
                // Waiting requests should fail like the request that ran the merge
                throw exception.cause ?: exception
            }
        }

        return try {
            mergeMetadata(repository, metadataFile, accessToken)
                .map { metadata ->
                    MergedMetadata(
                        content = metadataXmlMapper.writeValueAsBytes(metadata),
                        lastModifiedTime = metadata.versioning?.lastUpdated?.toInstant(),
                    )
                }
                .peek {
                    synchronized(cacheLock) {
                        if (repository.metadataMaxAgeInSeconds > 0 && generation == startedAt) merged.put(key, it)
                    }
                }
                .also { merge.result.complete(it) }
        } catch (throwable: Throwable) {
            merge.result.completeExceptionally(throwable)
            throw throwable
        } finally {
            synchronized(cacheLock) { inFlightMerges.remove(key, merge) }
        }
    }

    private fun mergeMetadata(repository: Repository, metadataFile: Location, accessToken: AccessTokenIdentifier?): Result<Metadata, ErrorResponse> {
        val local = when {
            repository.storageProvider.exists(metadataFile) -> listOf("local" to repository.storageProvider.getFile(metadataFile).map { data -> data.use { it.readBytes() } })
            else -> emptyList()
        }
        val remote = mirrorService
            .fetchFromAllRemoteHosts(repository, metadataFile, accessToken)
            .map { (host, result) -> host.host to result }

        val sources = mutableListOf<Metadata>()
        val failures = mutableListOf<ErrorResponse>()

        for ((source, result) in local + remote) {
            result
                .onError { error -> if (error.status != 404) failures.add(error) }
                .peek { content -> parse(source, metadataFile, content)?.let { sources.add(it) } }
        }

        if (sources.isEmpty()) {
            return failures.firstOrNull { it.status == 429 }?.let { Result.error(it) }
                ?: failures.lastOrNull()?.let { Result.error(it) }
                ?: notFoundError("Cannot find '$metadataFile' in local or remote repositories")
        }

        if (failures.isNotEmpty()) {
            logger.warn("MergedMetadata | Merged '$metadataFile' in ${repository.name} without ${failures.size} unavailable source(s): ${failures.joinToString { "${it.status}: ${it.message}" }}")
        }

        return MetadataMerger.merge(sources).asSuccess()
    }

    private fun parse(source: String, metadataFile: Location, content: ByteArray): Metadata? =
        runCatching { metadataXmlMapper.readValue<Metadata>(content) }
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
