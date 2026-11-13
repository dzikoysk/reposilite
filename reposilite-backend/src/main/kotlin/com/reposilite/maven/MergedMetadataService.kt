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
import com.reposilite.storage.filesystem.FileSystemStorageProvider
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.ContentType.TEXT_XML
import io.javalin.http.HttpStatus.NOT_FOUND
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
 *
 * The local file only contributes entries that are backed by files stored in the repository itself.
 * Clients update metadata by downloading the (merged) file and uploading it with their own entries,
 * and repositories may still hold copies of mirrored metadata that were stored before merging was enabled.
 * Both would otherwise keep entries of mirrors alive after they have been removed upstream.
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
    // Missing files are remembered like merged ones, so repeated lookups of unknown artifacts do not query all mirrors again
    private val merged: Cache<MergeKey, Result<MergedMetadata, ErrorResponse>> = Caffeine.newBuilder()
        .maximumSize(MAX_MERGED_ENTRIES.toLong())
        .ticker { TimeUnit.MILLISECONDS.toNanos(clock.millis()) }
        .expireAfter(object : Expiry<MergeKey, Result<MergedMetadata, ErrorResponse>> {
            override fun expireAfterCreate(key: MergeKey, value: Result<MergedMetadata, ErrorResponse>, currentTime: Long): Long =
                TimeUnit.SECONDS.toNanos(key.repository.metadataMaxAgeInSeconds.coerceAtLeast(0))

            override fun expireAfterUpdate(key: MergeKey, value: Result<MergedMetadata, ErrorResponse>, currentTime: Long, currentDuration: Long): Long =
                expireAfterCreate(key, value, currentTime)

            override fun expireAfterRead(key: MergeKey, value: Result<MergedMetadata, ErrorResponse>, currentTime: Long, currentDuration: Long): Long =
                currentDuration
        })
        .build()

    // Only cache bookkeeping is synchronized; fetching and waiting for mirrors happens outside this lock.
    private val cacheLock = Any()
    private val inFlightMerges = HashMap<MergeKey, InFlightMerge>()

    fun handles(repository: Repository, gav: Location): Boolean =
        repository.mergeMetadataEnabled && gav.isMetadataFileOrCompanion()

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
        // Creating or deleting an artifact can change which local versions, plugins or snapshot builds are backed by storage.
        val ancestors = generateSequence(gav.getParent()) { if (it == Location.empty()) null else it.getParent() }
            .map { it.resolve(METADATA_FILE) }
            .toHashSet()
        val subtree = if (gav == Location.empty()) "" else "$gav/"
        val affected = { key: MergeKey -> key.gav in ancestors || (recursive && key.gav.toString().startsWith(subtree)) }
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
        // Signatures and other companions served by a mirror would not match the merged content
        val metadataFile = gav.toMetadataFile() ?: return notFoundError("Merged metadata does not provide '${gav.getSimpleName()}'")

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
        merged.getIfPresent(key)?.let { return it }

        return when (val reservation = reserveMerge(key)) {
            is MergeReservation.Cached -> reservation.result
            is MergeReservation.Join -> awaitMerge(reservation.running)
            is MergeReservation.Run -> runMerge(key, reservation, accessToken)
        }
    }

    private sealed interface MergeReservation {
        data class Cached(val result: Result<MergedMetadata, ErrorResponse>) : MergeReservation
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
            val sources = findSources(key, accessToken)
            val result = sources.flatMap {
                if (it.isEmpty()) notFoundError("Cannot find '${key.gav}' in local or remote repositories")
                else serialize(MetadataMerger.merge(it)).asSuccess()
            }
            // A successful empty lookup is a confirmed miss. Parse and upstream errors must be retried.
            if (sources.isOk) publish(key, result, merge)
            merge.result.complete(result)
            result
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

    private fun publish(key: MergeKey, result: Result<MergedMetadata, ErrorResponse>, merge: InFlightMerge) {
        synchronized(cacheLock) {
            // Invalidation removes affected reservations; unrelated writes must not prevent this merge from being cached.
            if (key.repository.metadataMaxAgeInSeconds > 0 && inFlightMerges[key] === merge) {
                merged.put(key, result)
            }
        }
    }

    private fun findSources(key: MergeKey, accessToken: AccessTokenIdentifier?): Result<List<Metadata>, ErrorResponse> {
        val repository = key.repository
        val metadataFile = key.gav
        val sources = mutableListOf<Metadata>()
        val failures = mutableListOf<ErrorResponse>()

        readLocalMetadata(repository, metadataFile)
            .onError { failures.add(it) }
            .peek { sources.addAll(it) }

        for ((mirror, result) in mirrorService.fetchFromAllRemoteHosts(repository, metadataFile, accessToken)) {
            if (result.isErr && result.error.status == NOT_FOUND.code) continue
            result.flatMap { parse(mirror.host, metadataFile, it) }
                .onError { failures.add(it) }
                .peek { sources.add(it) }
        }

        if (failures.isNotEmpty()) {
            if (sources.isEmpty()) {
                val failure = failures.firstOrNull { it.status == 429 }
                    ?: failures.lastOrNull { it.status != NOT_FOUND.code }
                    ?: failures.last()
                return Result.error(failure)
            }
            logger.warn("MergedMetadata | Merged '$metadataFile' in ${repository.name} without ${failures.size} source(s): ${failures.joinToString { "${it.status}: ${it.message}" }}")
        }

        return sources.asSuccess()
    }

    private fun readLocalMetadata(repository: Repository, metadataFile: Location): Result<List<Metadata>, ErrorResponse> {
        val storage = repository.storageProvider

        if (!storage.exists(metadataFile)) {
            return Result.ok(emptyList())
        }

        return storage.getFile(metadataFile)
            .map { data -> data.use { it.readBytes() } }
            .flatMap { content -> parse("local", metadataFile, content) }
            .map { metadata -> listOfNotNull(retainStoredEntries(repository, metadataFile, metadata)) }
    }

    private fun retainStoredEntries(repository: Repository, metadataFile: Location, metadata: Metadata): Metadata? =
        repository.storageProvider.getFiles(metadataFile.getParent()).fold(
            { files -> MetadataMerger.retainStoredEntries(metadata, findStoredFiles(repository, metadata, files)) },
            { error ->
                logger.warn("MergedMetadata | Cannot verify entries of local '$metadataFile' in ${repository.name}: ${error.status}: ${error.message}")
                metadata
            }
        )

    /**
     * File systems keep the directory of a version after its last file has been deleted, so the directories of listed entries must not be empty.
     * Other storage providers do not have empty directories, which saves them a lookup per entry.
     */
    private fun findStoredFiles(repository: Repository, metadata: Metadata, files: List<Location>): Set<String> {
        val storage = repository.storageProvider
        val storedFiles = files.associateByTo(HashMap()) { it.getSimpleName() }

        if (storage is FileSystemStorageProvider) {
            val listed = metadata.versioning?.let { it.versions.orEmpty() + listOfNotNull(it.latest, it.release) }.orEmpty() +
                metadata.plugins.orEmpty().mapNotNull { it.artifactId }

            listed.toSet()
                .mapNotNull { name -> storedFiles[name]?.let { name to it } }
                .filter { (_, directory) -> runCatching { storage.getFiles(directory).fold({ it.isEmpty() }, { false }) }.getOrDefault(false) }
                .forEach { (name, _) -> storedFiles.remove(name) }
        }

        return storedFiles.keys
    }

    private fun parse(source: String, metadataFile: Location, content: ByteArray): Result<Metadata, ErrorResponse> =
        runCatching {
            readMetadataXml(content).also {
                require(it.matchesDirectory(metadataFile.getParent())) { "Declared coordinates do not match the requested path" }
            }
        }
            .fold({ it.asSuccess() }, {
                logger.warn("MergedMetadata | Skipping malformed '$metadataFile' from $source: ${it.message}")
                notFoundError("No valid metadata for '$metadataFile'")
            })

    private fun Metadata.matchesDirectory(directory: Location): Boolean {
        val group = groupId?.takeIf { it.isNotBlank() }?.replace('.', '/')
        fun matchesArtifact(path: Location): Boolean =
            (artifactId.isNullOrBlank() || artifactId == path.getSimpleName()) &&
                (group == null || group == path.getParent().toString())

        // Older clients and plugin markers also declare a version in the metadata of the artifact directory.
        if (!version.isNullOrBlank()) {
            return (version == directory.getSimpleName() && matchesArtifact(directory.getParent())) ||
                (!artifactId.isNullOrBlank() && matchesArtifact(directory))
        }
        if (artifactId.isNullOrBlank() && !plugins.isNullOrEmpty()) return group == null || group == directory.toString()
        // Release lists describe the artifact directory even when the artifactId is omitted.
        if (!versioning?.versions.isNullOrEmpty() || !versioning?.latest.isNullOrBlank() || !versioning?.release.isNullOrBlank()) {
            return matchesArtifact(directory)
        }
        if (!versioning?.snapshotVersions.isNullOrEmpty()) return directory.endsWith("-SNAPSHOT") && matchesArtifact(directory.getParent())
        if (artifactId.isNullOrBlank() && group == directory.toString()) return true
        return matchesArtifact(directory) || (directory.endsWith("-SNAPSHOT") && matchesArtifact(directory.getParent()))
    }

    private fun Location.toMetadataFile(): Location? =
        when (getSimpleName()) {
            METADATA_FILE -> this
            in checksumFileNames -> resolveSibling(METADATA_FILE)
            else -> null
        }

    private fun Location.isMetadataFileOrCompanion(): Boolean =
        getSimpleName().let { it == METADATA_FILE || it.startsWith("$METADATA_FILE.") }

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
