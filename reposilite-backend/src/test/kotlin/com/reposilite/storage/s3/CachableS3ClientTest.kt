package com.reposilite.storage.s3

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.util.concurrent.atomic.AtomicInteger

class CachableS3ClientTest {
    private lateinit var client: CachableS3Client
    private lateinit var headRequests: AtomicInteger

    @BeforeEach
    fun setUp() {
        headRequests = AtomicInteger(0)

        val s3Client = object: S3Client {
            override fun deleteObject(request: DeleteObjectRequest): DeleteObjectResponse {
                return DeleteObjectResponse.builder().build()
            }

            override fun headObject(request: HeadObjectRequest): HeadObjectResponse {
                headRequests.incrementAndGet()
                return HeadObjectResponse.builder().build()
            }

            override fun putObject(request: PutObjectRequest, requestBody: RequestBody?): PutObjectResponse {
                return PutObjectResponse.builder().build()
            }

            override fun serviceName(): String? {
                return S3Client.SERVICE_NAME
            }

            override fun close() {

            }
        }
        val cache: Cache<String, HeadObjectResponse> = Caffeine.newBuilder().maximumSize(100).build()
        client = CachableS3Client(s3Client, cache)
    }

    @Test
    fun `should cache headObject requests`() {
        val key = "foo/bar.txt"
        val request = HeadObjectRequest.builder().key(key).build()

        client.headObject(request)

        assertThat(headRequests.get()).isEqualTo(1)
        client.headObject(request)
        assertThat(headRequests.get()).isEqualTo(1)
    }

    @Test
    fun `should invalidate cache on deleteObject`() {
        val key = "foo/bar.txt"

        client.headObject(HeadObjectRequest.builder().key(key).build())
        client.headObject(HeadObjectRequest.builder().key(key).build())
        assertThat(headRequests.get()).isEqualTo(1)

        client.deleteObject(DeleteObjectRequest.builder().key(key).build())
        client.headObject(HeadObjectRequest.builder().key(key).build())
        assertThat(headRequests.get()).isEqualTo(2)
    }

    @Test
    fun `should invalidate cache on putObject`() {
        val key = "foo/bar.txt"

        client.headObject(HeadObjectRequest.builder().key(key).build())
        client.headObject(HeadObjectRequest.builder().key(key).build())
        assertThat(headRequests.get()).isEqualTo(1)

        client.putObject(PutObjectRequest.builder().key(key).build(), RequestBody.empty())
        client.headObject(HeadObjectRequest.builder().key(key).build())
        assertThat(headRequests.get()).isEqualTo(2)

    }

    @Test
    fun `should invalidate maven-metadata xml on putObject`() {
        val key = "parent/foo/bar.txt"
        val mavenMetadata1 = "parent/maven-metadata.xml"
        val mavenMetadata2 = "parent/maven-metadata.xml.sha512"

        client.headObject(HeadObjectRequest.builder().key(mavenMetadata1).build())
        client.headObject(HeadObjectRequest.builder().key(mavenMetadata2).build())
        assertThat(headRequests.get()).isEqualTo(2)

        client.putObject(PutObjectRequest.builder().key(key).build(), RequestBody.empty())

        client.headObject(HeadObjectRequest.builder().key(mavenMetadata1).build())
        client.headObject(HeadObjectRequest.builder().key(mavenMetadata2).build())
        assertThat(headRequests.get()).isEqualTo(4)
    }
}

