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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class MetadataMergerTest {

    @ParameterizedTest
    @CsvSource("1.0,1.0-sp", "1.0-rc2,1.0-rc10", "1.0-alpha,1.0-beta", "1.0-SNAPSHOT,1.0", "1.0-rc1,1.0")
    fun `should select latest and release using Maven version order`(older: String, newer: String) {
        val merged = MetadataMerger.merge(listOf(older, newer).map { version ->
            Metadata(versioning = Versioning(latest = version, _versions = listOf(version)))
        })

        assertThat(merged.versioning?.versions).containsExactly(older, newer)
        assertThat(merged.versioning?.latest).isEqualTo(newer)
        assertThat(merged.versioning?.release).isEqualTo(newer)
    }

    @ParameterizedTest
    @CsvSource("1.0,1.0-final", "1.0,1.0-ga", "1.0-RC1,1.0-rc1", "1,1.0.0")
    fun `should recognize equivalent Maven versions`(first: String, second: String) {
        assertThat(MetadataMerger.versionOrder.compare(first, second)).isZero()
    }

    @Test
    fun `should merge release and snapshot metadata`() {
        // given: release and snapshot metadata of the same artifact
        val releases = Metadata(
            groupId = "foo",
            artifactId = "bar",
            versioning = Versioning(latest = "4.0.0", release = "4.0.0", _versions = listOf("3.0.0", "4.0.0"), lastUpdated = "20260216073143")
        )
        val snapshots = Metadata(
            groupId = "foo",
            artifactId = "bar",
            versioning = Versioning(latest = "4.1.0-SNAPSHOT", _versions = listOf("3.0.0-SNAPSHOT", "4.0.0-SNAPSHOT", "4.1.0-SNAPSHOT"), lastUpdated = "20260219082051")
        )

        // when: both files are merged
        val merged = MetadataMerger.merge(listOf(releases, snapshots))

        // then: the result contains all versions and recalculated versioning fields
        assertThat(merged.groupId).isEqualTo("foo")
        assertThat(merged.artifactId).isEqualTo("bar")
        assertThat(merged.versioning?.versions).containsExactly("3.0.0-SNAPSHOT", "3.0.0", "4.0.0-SNAPSHOT", "4.0.0", "4.1.0-SNAPSHOT")
        assertThat(merged.versioning?.latest).isEqualTo("4.1.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("4.0.0")
        assertThat(merged.versioning?.lastUpdated).isEqualTo("20260219082051")
    }

    @Test
    fun `should prefer release over snapshot of the same version`() {
        // given: a snapshot repository that has not been cleaned up after a release
        val releases = Metadata(versioning = Versioning(latest = "2.0.0", _versions = listOf("2.0.0")))
        val snapshots = Metadata(versioning = Versioning(latest = "2.0.0-SNAPSHOT", _versions = listOf("2.0.0-SNAPSHOT")))

        // when: both files are merged
        val merged = MetadataMerger.merge(listOf(snapshots, releases))

        // then: the release is considered the latest version
        assertThat(merged.versioning?.latest).isEqualTo("2.0.0")
        assertThat(merged.versioning?.release).isEqualTo("2.0.0")
    }

    @Test
    fun `should derive versioning fields from versions if they are missing`() {
        // given: metadata files without latest and release declarations
        val first = Metadata(versioning = Versioning(_versions = listOf("1.0.0", "1.1.0-SNAPSHOT")))
        val second = Metadata(versioning = Versioning(_versions = listOf("1.0.1")))

        // when: both files are merged
        val merged = MetadataMerger.merge(listOf(first, second))

        // then: latest and release are calculated from the declared versions
        assertThat(merged.versioning?.latest).isEqualTo("1.1.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("1.0.1")
    }

    @Test
    fun `should recalculate latest and release from the complete version list`() {
        val first = Metadata(versioning = Versioning(
            latest = "1.0", release = "1.0", _versions = listOf("1.0", "2.0", "3.0-SNAPSHOT"),
        ))
        val second = Metadata(versioning = Versioning(
            latest = "99.0", release = "99.0", _versions = listOf("0.9"),
        ))

        val merged = MetadataMerger.merge(listOf(first, second))

        assertThat(merged.versioning?.latest).isEqualTo("3.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("2.0")
    }

    @Test
    fun `should normalize even a single metadata source`() {
        val source = Metadata(versioning = Versioning(_versions = listOf("2.0-SNAPSHOT", "1.0", "1.0")))

        val merged = MetadataMerger.merge(listOf(source))

        assertThat(merged.versioning?.versions).containsExactly("1.0", "2.0-SNAPSHOT")
        assertThat(merged.versioning?.latest).isEqualTo("2.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("1.0")
        assertThat(source.versioning?.versions).containsExactly("2.0-SNAPSHOT", "1.0", "1.0")
    }

    @Test
    fun `should omit release if only snapshot versions are listed`() {
        val source = Metadata(versioning = Versioning(release = "1.0", _versions = listOf("2.0-SNAPSHOT")))

        val merged = MetadataMerger.merge(listOf(source))

        assertThat(merged.versioning?.latest).isEqualTo("2.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isNull()
    }

    @Test
    fun `should retain explicit versioning fields when no version list is available`() {
        val source = Metadata(versioning = Versioning(latest = "2.0-SNAPSHOT", release = "1.0"))

        val merged = MetadataMerger.merge(listOf(source))

        assertThat(merged.versioning?.latest).isEqualTo("2.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("1.0")
    }

    @ParameterizedTest
    @CsvSource("20260201120000,20260201120000", "20260203120000,20260201120000")
    fun `should select snapshot builds by version rather than updated timestamp`(olderUpdated: String, newerUpdated: String) {
        val older = Metadata(version = "1.0-SNAPSHOT", versioning = Versioning(_snapshotVersions = listOf(
            SnapshotVersion(extension = "jar", value = "1.0-20260201.120000-1", updated = olderUpdated),
        )))
        val newer = Metadata(version = "1.0-SNAPSHOT", versioning = Versioning(_snapshotVersions = listOf(
            SnapshotVersion(extension = "jar", value = "1.0-20260201.120000-2", updated = newerUpdated),
        )))

        for (sources in listOf(listOf(older, newer), listOf(newer, older))) {
            val merged = MetadataMerger.merge(sources)

            assertThat(merged.versioning?.snapshotVersions?.single()).isEqualTo(newer.versioning?.snapshotVersions?.single())
        }
    }

    @Test
    fun `should merge snapshot builds per classifier and extension`() {
        // given: version-level snapshot metadata published to two repositories, sources only published to the older one
        val olderSources = SnapshotVersion(classifier = "sources", extension = "jar", value = "1.0.0-20260101.120000-3", updated = "20260101120000")
        val older = Metadata(
            version = "1.0.0-SNAPSHOT",
            versioning = Versioning(
                snapshot = Snapshot(timestamp = "20260101.120000", buildNumber = 3),
                _snapshotVersions = listOf(
                    SnapshotVersion(extension = "jar", value = "1.0.0-20260101.120000-3", updated = "20260101120000"),
                    olderSources,
                ),
                lastUpdated = "20260101120000"
            )
        )
        val newer = Metadata(
            version = "1.0.0-SNAPSHOT",
            versioning = Versioning(
                snapshot = Snapshot(timestamp = "20260202.120000", buildNumber = 1),
                _snapshotVersions = listOf(SnapshotVersion(extension = "jar", value = "1.0.0-20260202.120000-1", updated = "20260202120000")),
                lastUpdated = "20260202120000"
            )
        )

        // when: both files are merged
        val merged = MetadataMerger.merge(listOf(older, newer))

        // then: every classifier points to its newest build available in any repository
        assertThat(merged.version).isEqualTo("1.0.0-SNAPSHOT")
        assertThat(merged.versioning?.snapshot).isEqualTo(newer.versioning?.snapshot)
        assertThat(merged.versioning?.snapshotVersions).containsExactlyInAnyOrder(newer.versioning!!.snapshotVersions!!.first(), olderSources)
        assertThat(merged.versioning?.lastUpdated).isEqualTo("20260202120000")
    }

    @Test
    fun `should merge group-level plugin metadata`() {
        // given: plugin group metadata from two repositories
        val first = Metadata(versioning = null).also { it.plugins = listOf(Plugin("A", "a", "a-maven-plugin")) }
        val second = Metadata(versioning = null).also { it.plugins = listOf(Plugin("A", "a", "a-maven-plugin"), Plugin("B", "b", "b-maven-plugin")) }

        // when: both files are merged
        val merged = MetadataMerger.merge(listOf(first, second))

        // then: plugins are combined without duplicates
        assertThat(merged.plugins).containsExactly(Plugin("A", "a", "a-maven-plugin"), Plugin("B", "b", "b-maven-plugin"))
        assertThat(merged.versioning).isNull()
    }

}
