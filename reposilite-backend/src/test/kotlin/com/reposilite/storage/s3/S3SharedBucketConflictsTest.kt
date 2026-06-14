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

package com.reposilite.storage.s3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class S3SharedBucketConflictsTest {

    private fun repository(id: String, bucket: String, endpoint: String = "", sharedBucket: Boolean = false): Pair<String, S3StorageProviderSettings> =
        id to S3StorageProviderSettings(bucketName = bucket, endpoint = endpoint, sharedBucket = sharedBucket)

    @Test
    fun `should report no conflicts when each repository targets its own bucket`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "releases-bucket"),
            repository("snapshots", "snapshots-bucket"),
        ))

        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should report no conflicts when repositories share a bucket in single-bucket mode`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "shared", sharedBucket = true),
            repository("snapshots", "shared", sharedBucket = true),
        ))

        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should report both repositories when they share a bucket without single-bucket mode`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "shared"),
            repository("snapshots", "shared"),
        ))

        assertThat(conflicts).containsExactlyInAnyOrder("releases", "snapshots")
    }

    @Test
    fun `should report only the repository that opts out of single-bucket mode on a shared bucket`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "shared", sharedBucket = true),
            repository("snapshots", "shared", sharedBucket = false),
        ))

        assertThat(conflicts).containsExactly("snapshots")
    }

    @Test
    fun `should not treat the same bucket name on different endpoints as a conflict`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "bucket", endpoint = "https://a.example"),
            repository("snapshots", "bucket", endpoint = "https://b.example"),
        ))

        assertThat(conflicts).isEmpty()
    }

    @Test
    fun `should detect a conflict when endpoint and bucket differ only by trailing slash or whitespace`() {
        val conflicts = findS3SharedBucketConflicts(listOf(
            repository("releases", "shared", endpoint = "https://s3.example"),
            repository("snapshots", " shared ", endpoint = "https://s3.example/"),
        ))

        assertThat(conflicts).containsExactlyInAnyOrder("releases", "snapshots")
    }
}
