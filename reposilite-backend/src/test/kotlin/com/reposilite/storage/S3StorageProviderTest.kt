package com.reposilite.storage

import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.storage.infrastructure.S3StorageProvider
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Testcontainers
internal class S3StorageProviderTest : StorageProviderTest() {

    @Container
    val localstack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.17"))
        .withServices(S3)

    @BeforeEach
    fun setup() {
        val s3 = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.accessKey, localstack.secretKey
                    )
                )
            )
            .region(Region.of(localstack.region))
            .build()

        this.storageProvider = S3StorageProvider(InMemoryLogger(), s3, "repositories")
    }

}