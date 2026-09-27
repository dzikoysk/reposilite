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

package com.reposilite.storage.s3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class S3SharedBucketConflictsTest {

    @Test
    fun `should allow distinct buckets endpoints and prefixes`() {
        // given: repositories using different S3 locations
        val repositories = listOf(
            "releases" to S3StorageProviderSettings(endpoint = "https://s3.example", bucketName = "releases", prefix = "packages"),
            "snapshots" to S3StorageProviderSettings(endpoint = "https://s3.example", bucketName = "snapshots"),
            "downloads" to S3StorageProviderSettings(endpoint = "https://other.example", bucketName = "releases"),
            "assets" to S3StorageProviderSettings(endpoint = "https://s3.example", bucketName = "releases", prefix = "assets"),
        )

        // when: the complete configuration is validated
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: independent storage locations are accepted
        assertThat(conflicts).isEmpty()
    }

    @ParameterizedTest
    @CsvSource("artifacts,artifacts", "artifacts,artifacts/releases", "artifacts/releases,artifacts", "'',artifacts")
    fun `should detect overlapping storage in either order`(first: String, second: String) {
        // given: repositories sharing the same or a nested S3 path
        val repositories = listOf(
            "releases" to S3StorageProviderSettings(bucketName = "shared", prefix = first),
            "downloads" to S3StorageProviderSettings(bucketName = "shared", prefix = second),
        )

        // when: storage is validated without creating any clients
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: both repositories are reported
        assertThat(conflicts).containsExactlyInAnyOrder("releases", "downloads")
    }

    @Test
    fun `should normalize endpoint bucket and prefix before comparison`() {
        // given: equivalent paths written differently
        val repositories = listOf(
            "releases" to S3StorageProviderSettings(endpoint = "https://s3.example", bucketName = "shared", prefix = "artifacts"),
            "downloads" to S3StorageProviderSettings(endpoint = " https://s3.example/ ", bucketName = " shared ", prefix = "/artifacts/"),
        )

        // when: storage is validated
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: formatting does not hide the overlap
        assertThat(conflicts).containsExactlyInAnyOrder("releases", "downloads")
    }

    @Test
    fun `should use repository names only when shared bucket is enabled`() {
        // given: repositories sharing a bucket with separate name-based prefixes
        val storage = S3StorageProviderSettings(bucketName = "shared", sharedBucket = true)
        val repositories = listOf("releases" to storage, "releases-extra" to storage)

        // when: storage is validated
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: complete path segments keep the repositories separate
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should ignore unconfigured buckets`() {
        // given: incomplete S3 configurations
        val repositories = listOf("first" to S3StorageProviderSettings(), "second" to S3StorageProviderSettings())

        // when: their storage locations are compared
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: missing bucket settings are left to individual repository initialization
        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect overlapping repositories with the same name`() {
        // given: different repository types using the same name and storage
        val storage = S3StorageProviderSettings(bucketName = "shared")
        val repositories = listOf("releases" to storage, "releases" to storage)

        // when: storage locations are compared
        val conflicts = findS3SharedBucketConflicts(repositories)

        // then: a shared name does not hide overlapping storage
        assertThat(conflicts).containsExactly("releases")
    }
}
