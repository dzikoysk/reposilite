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
import com.reposilite.maven.api.Snapshot
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

        val plugins = sources
            .flatMap { it.plugins.orEmpty() }
            .distinctBy { it.prefix to it.artifactId }
            .takeIf { it.isNotEmpty() }

        return Metadata(
            groupId = sources.firstNotNullOfOrNull { it.groupId },
            artifactId = sources.firstNotNullOfOrNull { it.artifactId },
            version = sources.firstNotNullOfOrNull { it.version },
            versioning = mergeVersioning(sources.mapNotNull { it.versioning }),
        ).also { it.plugins = plugins }
    }

    private fun mergeVersioning(sources: List<Versioning>): Versioning? {
        if (sources.isEmpty()) {
            return null
        }

        val versions = sources
            .flatMap { it.versions.orEmpty() }
            .distinct()
            .sortedWith(versionOrder)

        // Each classifier/extension points to its highest snapshot version, as in Nexus groups.
        val snapshot = sources
            .mapNotNull { it.snapshot }
            .maxWithOrNull(compareBy<Snapshot> { it.timestamp.orEmpty() }.thenBy { it.buildNumber ?: 0 })

        val snapshotVersions = sources
            .flatMap { it.snapshotVersions.orEmpty() }
            .groupBy { it.classifier.orEmpty() to it.extension.orEmpty() }
            .values
            .map { builds -> builds.maxWith(compareBy(versionOrder) { it.value.orEmpty() }) }
            .takeIf { it.isNotEmpty() }

        return Versioning(
            latest = versions.lastOrNull() ?: sources.mapNotNull { it.latest }.maxWithOrNull(versionOrder),
            release = when {
                versions.isNotEmpty() -> versions.lastOrNull { !it.endsWith(SNAPSHOT_SUFFIX) }
                else -> sources.mapNotNull { it.release }.maxWithOrNull(versionOrder)
            },
            _versions = versions.takeIf { it.isNotEmpty() },
            snapshot = snapshot,
            _snapshotVersions = snapshotVersions,
            lastUpdated = sources.mapNotNull { it.lastUpdated }.maxOrNull(),
        )
    }

}
