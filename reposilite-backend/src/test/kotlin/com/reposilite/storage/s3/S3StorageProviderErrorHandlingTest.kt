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

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.status.application.FailureComponents
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.toLocation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.S3Object

internal class S3StorageProviderErrorHandlingTest {

    private val logger = InMemoryLogger()
    private val failureFacade = FailureComponents(journalist = logger).failureFacade()

    private class FakeS3Client(
        private val onHeadObject: (HeadObjectRequest) -> HeadObjectResponse,
        private val keys: List<String> = emptyList(),
    ) : S3Client {

        override fun headBucket(request: HeadBucketRequest): HeadBucketResponse =
            HeadBucketResponse.builder().build()

        override fun headObject(request: HeadObjectRequest): HeadObjectResponse =
            onHeadObject(request)

        override fun listObjectsV2(request: ListObjectsV2Request): ListObjectsV2Response =
            ListObjectsV2Response.builder()
                .contents(keys.filter { it.startsWith(request.prefix()) }.map { S3Object.builder().key(it).size(1).build() })
                .isTruncated(false)
                .build()

        override fun serviceName(): String =
            S3Client.SERVICE_NAME

        override fun close() {}

    }

    private fun createProvider(client: S3Client): S3StorageProvider =
        S3StorageProvider(failureFacade = failureFacade, s3 = client, bucket = "test")

    @Test
    fun `should not report an existing file as missing when HEAD fails for a transient reason`() {
        // given: an existing file, but HEAD fails
        val provider = createProvider(FakeS3Client(
            onHeadObject = { throw SdkClientException.create("Unable to execute HTTP request: Connection reset") },
            keys = listOf("com/example/lib/1.0.0/lib-1.0.0.jar"),
        ))

        // when: details of the file are requested
        val result = provider.getFileDetails("com/example/lib/1.0.0/lib-1.0.0.jar".toLocation())

        // then: a server error is returned and recorded
        assertThat(assertError(result).status).isEqualTo(500)
        assertThat(failureFacade.hasFailures()).isTrue
    }

    @Test
    fun `should not report an existing file as missing when S3 answers HEAD with 5xx`() {
        // given: HEAD returns 503
        val provider = createProvider(FakeS3Client(
            onHeadObject = { throw S3Exception.builder().statusCode(503).message("Slow Down").build() },
        ))

        // when: details of the file are requested
        val result = provider.getFileDetails("com/example/lib/1.0.0/lib-1.0.0.jar".toLocation())

        // then: a server error is returned
        assertThat(assertError(result).status).isEqualTo(500)
    }

    @Test
    fun `should still fall back to directory listing when HEAD reports a missing key`() {
        // given: a directory
        val provider = createProvider(FakeS3Client(
            onHeadObject = { throw NoSuchKeyException.builder().statusCode(404).build() },
            keys = listOf("com/example/lib/1.0.0/lib-1.0.0.jar"),
        ))

        // when: details of the directory are requested
        val result = provider.getFileDetails("com/example/lib".toLocation())

        // then: it is resolved as a directory
        assertThat(assertOk(result)).isInstanceOf(DirectoryInfo::class.java)
    }

    @Test
    fun `should treat HEAD 404 without error body as a missing key`() {
        // given: HEAD returns 404 without a NoSuchKey error
        val provider = createProvider(FakeS3Client(
            onHeadObject = { throw S3Exception.builder().statusCode(404).build() },
            keys = listOf("com/example/lib/1.0.0/lib-1.0.0.jar"),
        ))

        // when: a directory and a missing file are requested
        val directory = provider.getFileDetails("com/example/lib".toLocation())
        val missing = provider.getFileDetails("com/example/lib/1.0.0/missing.jar".toLocation())

        // then: the directory is listed and the file is missing
        assertThat(assertOk(directory)).isInstanceOf(DirectoryInfo::class.java)
        assertThat(assertError(missing).status).isEqualTo(404)
    }

}
