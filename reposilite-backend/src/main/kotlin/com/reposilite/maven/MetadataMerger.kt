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

import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.Plugin
import com.reposilite.maven.api.Snapshot
import com.reposilite.maven.api.SnapshotVersion
import com.reposilite.maven.api.Versioning
import org.apache.maven.artifact.versioning.ComparableVersion

private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"

/**
 * Combines maven-metadata.xml files of the same location served by multiple repositories.
 */
internal object MetadataMerger {

    internal val versionOrder: Comparator<String> = compareBy { ComparableVersion(it) }

    fun merge(sources: List<Metadata>): Metadata {
        require(sources.isNotEmpty()) { "At least one metadata source is required" }

        return Metadata(
            groupId = sources.firstNotNullOfOrNull { it.groupId },
            artifactId = sources.firstNotNullOfOrNull { it.artifactId },
            version = sources.firstNotNullOfOrNull { it.version },
            versioning = mergeVersioning(sources.mapNotNull { it.versioning }),
        ).also { it.plugins = mergePlugins(sources) }
    }

    /**
     * Reduces metadata to the entries backed by the given files and directories stored next to it:
     * versions and plugins with a directory, and snapshot builds with a file of the given build.
     * Returns null if nothing is left.
     */
    fun retainStoredEntries(metadata: Metadata, storedFiles: Set<String>): Metadata? {
        val versioning = metadata.versioning?.let {
            Versioning(
                release = it.release?.takeIf { release -> release in storedFiles },
                latest = it.latest?.takeIf { latest -> latest in storedFiles },
                _versions = it.versions?.filter { version -> version in storedFiles }?.takeIf { versions -> versions.isNotEmpty() },
                snapshot = it.snapshot?.takeIf { snapshot -> snapshot.isStored(storedFiles) },
                _snapshotVersions = it.snapshotVersions?.filter { build -> build.isStored(storedFiles) }?.takeIf { builds -> builds.isNotEmpty() },
                lastUpdated = it.lastUpdated,
            )
        }
        val plugins = metadata.plugins?.filter { it.artifactId in storedFiles }?.takeIf { it.isNotEmpty() }

        val hasVersions = versioning != null && (versioning.latest != null || versioning.release != null ||
            versioning.versions != null || versioning.snapshot != null || versioning.snapshotVersions != null)
        if (!hasVersions && plugins == null) return null
        return metadata.copy(versioning = versioning).also { it.plugins = plugins }
    }

    // Non-unique snapshots do not declare a timestamp, so there is no file to look for.
    // An element without any value is what Reposilite writes for metadata that has no snapshot at all.
    private fun Snapshot.isStored(storedFiles: Set<String>): Boolean {
        if (timestamp == null) return buildNumber != null || localCopy == true
        val build = "-$timestamp" + buildNumber?.let { "-$it" }.orEmpty()
        return storedFiles.any { file -> file.endsWith(build) || "$build." in file || "$build-" in file }
    }

    private fun SnapshotVersion.isStored(storedFiles: Set<String>): Boolean {
        val suffix = "-$value" + classifier?.takeIf { it.isNotEmpty() }?.let { "-$it" }.orEmpty() + extension?.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        return value != null && storedFiles.any { it.endsWith(suffix) }
    }

    private fun mergePlugins(sources: List<Metadata>): List<Plugin>? =
        sources
            .flatMap { it.plugins.orEmpty() }
            .distinctBy { it.prefix to it.artifactId }
            .takeIf { it.isNotEmpty() }

    private fun mergeVersioning(sources: List<Versioning>): Versioning? {
        if (sources.isEmpty()) {
            return null
        }

        val versions = mergeVersions(sources)

        return Versioning(
            latest = versions.lastOrNull(),
            release = versions.lastOrNull { !it.endsWith(SNAPSHOT_SUFFIX) },
            _versions = versions.takeIf { it.isNotEmpty() },
            snapshot = findNewestSnapshot(sources),
            _snapshotVersions = findNewestSnapshotVersions(sources),
            lastUpdated = sources.mapNotNull { it.lastUpdated }.maxOrNull(),
        )
    }

    private fun mergeVersions(sources: List<Versioning>): List<String> =
        sources
            .flatMap { source ->
                source.versions.orEmpty().ifEmpty { listOfNotNull(source.latest, source.release) }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(versionOrder)

    private fun findNewestSnapshot(sources: List<Versioning>): Snapshot? =
        sources
            .mapNotNull { it.snapshot }
            .maxWithOrNull(compareBy<Snapshot> { it.timestamp.orEmpty() }.thenBy { it.buildNumber ?: 0 })

    // Each classifier/extension points to its highest snapshot version, as in Nexus groups.
    private fun findNewestSnapshotVersions(sources: List<Versioning>): List<SnapshotVersion>? =
        sources
            .flatMap { it.snapshotVersions.orEmpty() }
            .groupBy { it.classifier.orEmpty() to it.extension.orEmpty() }
            .values
            .map { builds -> builds.maxWith(compareBy(versionOrder) { it.value.orEmpty() }) }
            .takeIf { it.isNotEmpty() }

}
