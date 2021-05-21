/*
 * Copyright (c) 2021 dzikoysk
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
package org.panda_lang.reposilite.maven.metadata

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.apache.http.HttpStatus
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.maven.repository.Repository
import org.panda_lang.reposilite.shared.utils.ArrayUtils
import org.panda_lang.reposilite.shared.utils.FilesUtils
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.collection.Pair
import org.panda_lang.utilities.commons.function.Lazy
import org.panda_lang.utilities.commons.function.Result
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

// TODO: Simplify this trash class
class MetadataFacade(private val failureFacade: FailureFacade) {

    private val metadataCache: MutableMap<Path, Pair<FileDetailsResponse, String>> = ConcurrentHashMap()

    companion object {
        private val XML_MAPPER: Lazy<XmlMapper> = Lazy(Supplier {
            XmlMapper.xmlBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .defaultUseWrapper(false)
                .build()
        })
    }

    fun getMetadata(repository: Repository, requested: Path): Result<Pair<FileDetailsResponse, String>, ErrorResponse> {
        if (requested.fileName.toString() != "maven-metadata.xml") {
            return Result.error<Pair<FileDetailsResponse, String>?, ErrorResponse>(ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Bad request"))
        }

        val cachedContent: Pair<FileDetailsResponse, String>? = metadataCache[requested]

        if (cachedContent != null) {
            return Result.ok<Pair<FileDetailsResponse, String>?, ErrorResponse>(cachedContent)
        }

        val artifactDirectory = requested.parent

        if (repository.exists(artifactDirectory)) {
            return Result.error<Pair<FileDetailsResponse, String>?, ErrorResponse>(ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Bad request"))
        }

        val versions: Result<Array<Path>, ErrorResponse> = MetadataUtils.toSortedVersions(repository, artifactDirectory)

        if (versions.isErr) {
            return versions.map { null } // ?
        }

        return if (versions.get().isNotEmpty()) {
            generateArtifactMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory, versions.get())
        }
        else generateBuildMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory)
    }

    private fun generateArtifactMetadata(
        repository: Repository,
        metadataFile: Path,
        groupId: String,
        artifactDirectory: Path,
        versions: Array<Path>
    ): Result<Pair<FileDetailsResponse, String>, ErrorResponse> {
        val latest = ArrayUtils.getFirst(versions)!!

        val versioning = Versioning(
            latest.fileName.toString(),
            latest.fileName.toString(),
            FilesUtils.toNames(versions),
            null,
            null,
            MetadataUtils.toUpdateTime(repository, latest)
        )
        val metadata = Metadata(groupId, artifactDirectory.fileName.toString(), null, versioning)
        return toMetadataFile(repository, metadataFile, metadata)
    }

    private fun generateBuildMetadata(
        repository: Repository,
        metadataFile: Path,
        groupId: String,
        versionDirectory: Path
    ): Result<Pair<FileDetailsResponse, String>, ErrorResponse> {
        val artifactDirectory = versionDirectory.parent
        val builds: Result<Array<Path>, ErrorResponse> = MetadataUtils.toSortedBuilds(repository, versionDirectory)

        if (builds.isErr) {
            return builds.map { null }
        }

        val latestBuild = ArrayUtils.getFirst(builds.get())
            ?: return Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "Latest build not found"))

        val name = artifactDirectory.fileName.toString()
        val version = StringUtils.replace(versionDirectory.fileName.toString(), "-SNAPSHOT", StringUtils.EMPTY)
        val identifiers = MetadataUtils.toSortedIdentifiers(repository, name, version, builds.get())
        val latestIdentifier = Objects.requireNonNull(ArrayUtils.getFirst(identifiers))
        val buildSeparatorIndex = latestIdentifier!!.lastIndexOf("-")

        // snapshot requests
        val versioning: Versioning = if (buildSeparatorIndex != -1) {
            // format: timestamp-buildNumber
            val latestTimestamp = latestIdentifier.substring(0, buildSeparatorIndex)
            val latestBuildNumber = latestIdentifier.substring(buildSeparatorIndex + 1)
            val snapshot = Snapshot(latestTimestamp, latestBuildNumber)
            val snapshotVersions: MutableCollection<SnapshotVersion> = ArrayList<SnapshotVersion>(builds.get().size)

            for (identifier in identifiers) {
                val buildFiles = MetadataUtils.toBuildFiles(repository, versionDirectory, identifier)

                for (buildFile in buildFiles) {
                    val fileName = buildFile.fileName.toString()
                    val value = "$version-$identifier"
                    val updated = MetadataUtils.toUpdateTime(repository, buildFile)
                    val extension = fileName
                        .replace("$name-", StringUtils.EMPTY)
                        .replace("$value.", StringUtils.EMPTY)
                    val snapshotVersion = SnapshotVersion(extension, value, updated)

                    snapshotVersions.add(snapshotVersion)
                }
            }

            Versioning(null, null, null, snapshot, snapshotVersions, MetadataUtils.toUpdateTime(repository, latestBuild))
        } else {
            val fullVersion = "$version-SNAPSHOT"
            Versioning(fullVersion, fullVersion, listOf(fullVersion), null, null, MetadataUtils.toUpdateTime(repository, latestBuild))
        }

        return toMetadataFile(repository, metadataFile, Metadata(groupId, name, versionDirectory.fileName.toString(), versioning))
    }

    private fun toMetadataFile(repository: Repository, metadataFile: Path, metadata: Metadata): Result<Pair<FileDetailsResponse, String>, ErrorResponse> {
        return try {
            val serializedMetadata: String = XML_MAPPER.get().writeValueAsString(metadata)
            val bytes = serializedMetadata.toByteArray(StandardCharsets.UTF_8)
            val result: Result<FileDetailsResponse, ErrorResponse> = repository.putFile(metadataFile, bytes)

            if (result.isOk) {
                FilesUtils.writeFileChecksums(repository, metadataFile, bytes)
                metadataCache[metadataFile] = Pair<FileDetailsResponse, String>(result.get(), serializedMetadata)
            }

            result.map { fileDetailsResponse: FileDetailsResponse ->
                Pair<FileDetailsResponse, String>(
                    fileDetailsResponse,
                    serializedMetadata
                )
            }
        } catch (ioException: IOException) {
            failureFacade.throwException(metadataFile.toAbsolutePath().toString(), ioException)
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cannot generate metadata"))
        }
    }

    fun clearMetadata(metadataFile: Path) {
        metadataCache.remove(metadataFile)
    }

    fun purgeCache(): Int {
        val count = getCacheSize()
        metadataCache.clear()
        return count
    }

    fun getCacheSize(): Int {
        return metadataCache.size
    }

}