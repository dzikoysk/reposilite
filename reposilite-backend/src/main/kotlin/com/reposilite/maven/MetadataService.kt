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
package com.reposilite.maven

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.BAD_REQUEST
import com.reposilite.failure.FailureFacade
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.shared.FilesUtils
import com.reposilite.shared.getSimpleName
import com.reposilite.web.error.ErrorResponse
import com.reposilite.web.error.errorResponse
import panda.std.Lazy
import panda.std.Pair
import panda.std.Result
import panda.utilities.StringUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

// TOFIX: Simplify this trash class
internal class MetadataService(private val failureFacade: FailureFacade) {

    private val metadataCache: MutableMap<Path, Pair<DocumentInfo, String>> = ConcurrentHashMap()

    companion object {

        private val XML_MAPPER = Lazy {
            XmlMapper.xmlBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .defaultUseWrapper(false)
                .build()
        }

    }

    fun getMetadata(repository: Repository, requested: Path): Result<Pair<DocumentInfo, String>, ErrorResponse> {
        if (requested.getSimpleName() != "maven-metadata.xml") {
            return errorResponse(BAD_REQUEST, "Bad request")
        }

        val cachedContent = metadataCache[requested]

        if (cachedContent != null) {
            return Result.ok(cachedContent)
        }

        val artifactDirectory = requested.parent

        if (repository.exists(artifactDirectory)) {
            return errorResponse(BAD_REQUEST, "Bad request")
        }

        val versions = MetadataUtils.toSortedVersions(repository, artifactDirectory)

        if (versions.isErr) {
            return versions.projectToError()
        }

        return if (versions.get().isNotEmpty()) {
            generateArtifactMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory, versions.get())
        }
        else {
            generateBuildMetadata(repository, requested, MetadataUtils.toGroup(requested), artifactDirectory)
        }
    }

    private fun generateArtifactMetadata(
        repository: Repository,
        metadataFile: Path,
        groupId: String,
        artifactDirectory: Path,
        versions: List<Path>
    ): Result<Pair<DocumentInfo, String>, ErrorResponse> {
        val latest = versions.first()

        val versioning = Versioning(
            latest.getSimpleName(),
            latest.getSimpleName(),
            FilesUtils.toNames(versions),
            null,
            null,
            MetadataUtils.toUpdateTime(repository, latest)
        )

        val metadata = Metadata(groupId, artifactDirectory.getSimpleName(), null, versioning)
        return toMetadataFile(repository, metadataFile, metadata)
    }

    private fun generateBuildMetadata(
        repository: Repository,
        metadataFile: Path,
        groupId: String,
        versionDirectory: Path
    ): Result<Pair<DocumentInfo, String>, ErrorResponse> {
        val artifactDirectory = versionDirectory.parent
        val builds = MetadataUtils.toSortedBuilds(repository, versionDirectory)

        if (builds.isErr) {
            return builds.projectToError()
        }

        val latestBuild = builds.get().firstOrNull()
            ?: return errorResponse(HttpCode.NOT_FOUND, "Latest build not found")

        val name = artifactDirectory.fileName.toString()
        val version = StringUtils.replace(versionDirectory.fileName.toString(), "-SNAPSHOT", StringUtils.EMPTY)
        val identifiers = MetadataUtils.toSortedIdentifiers(name, version, builds.get())
        val latestIdentifier = identifiers.first()
        val buildSeparatorIndex = latestIdentifier.lastIndexOf("-")

        // snapshot requests
        val versioning: Versioning = if (buildSeparatorIndex != -1) {
            // format: timestamp-buildNumber
            val latestTimestamp = latestIdentifier.substring(0, buildSeparatorIndex)
            val latestBuildNumber = latestIdentifier.substring(buildSeparatorIndex + 1)
            val snapshot = Snapshot(latestTimestamp, latestBuildNumber)
            val snapshotVersions: MutableCollection<SnapshotVersion> = ArrayList(builds.get().size)

            for (identifier in identifiers) {
                val buildFiles = MetadataUtils.toBuildFiles(repository, versionDirectory, identifier)

                for (buildFile in buildFiles) {
                    val fileName = buildFile.getSimpleName()
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
        }
        else {
            val fullVersion = "$version-SNAPSHOT"
            Versioning(fullVersion, fullVersion, listOf(fullVersion), null, null, MetadataUtils.toUpdateTime(repository, latestBuild))
        }

        return toMetadataFile(repository, metadataFile, Metadata(groupId, name, versionDirectory.getSimpleName(), versioning))
    }

    private fun toMetadataFile(repository: Repository, metadataFile: Path, metadata: Metadata): Result<Pair<DocumentInfo, String>, ErrorResponse> {
        return try {
            val serializedMetadata: String = XML_MAPPER.get().writeValueAsString(metadata)
            val bytes = serializedMetadata.toByteArray(StandardCharsets.UTF_8)

            repository.putFile(metadataFile, bytes)
                .map { Pair(it, serializedMetadata) }
                .peek {
                    FilesUtils.writeFileChecksums(repository, metadataFile, bytes)
                    metadataCache[metadataFile] = it
                }
        }
        catch (ioException: IOException) {
            failureFacade.throwException(metadataFile.toAbsolutePath().toString(), ioException)
            errorResponse(HttpCode.INTERNAL_SERVER_ERROR, "Cannot generate metadata")
        }
    }

    fun clearMetadata(metadataFile: Path) =
        metadataCache.remove(metadataFile)

    fun purgeCache(): Int =
        getCacheSize().also { metadataCache.clear() }

    fun getCacheSize(): Int =
        metadataCache.size

}