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

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.reposilite.journalist.Journalist
import com.reposilite.shared.maskSecret
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderFactory
import com.reposilite.storage.StorageProviderOwner
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.signer.AwsS3V4Signer
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import java.net.URI
import java.nio.file.Path
import java.time.Duration

private val useLegacyS3V4Signer = System.getProperty("reposilite.s3.use-s3-v4-signer", "false") == "true"

enum class S3Signer {
    DEFAULT,
    LEGACY_V4
}

class S3StorageProviderFactory : StorageProviderFactory<S3StorageProvider, S3StorageProviderSettings> {

    private val namespaceClaims = S3NamespaceClaims()

    override fun create(
        journalist: Journalist,
        failureFacade: FailureFacade,
        workingDirectory: Path,
        owner: StorageProviderOwner,
        settings: S3StorageProviderSettings
    ): S3StorageProvider {
        val client = S3Client.builder()

        val pathStyleAccessEnabled = System.getProperty("reposilite.s3.pathStyleAccessEnabled") == "true"
        if (pathStyleAccessEnabled) {
            client.serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
        }

        if (settings.accessKey.isNotEmpty() && settings.secretKey.isNotEmpty()) {
            client.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        settings.accessKey,
                        settings.secretKey
                    )
                )
            )
        }

        // The AWS Java SDK uses by default the env variable AWS_REGION to determine the region
        // We use reposilite as a dummy region for S3-compatible providers, see https://github.com/dzikoysk/reposilite/issues/1666
        val region = Region.of(when {
            settings.region.isNotEmpty() -> settings.region
            else -> System.getenv("AWS_REGION") ?: "reposilite"
        })

        client.region(region)

        val customEndpoint = settings.endpoint
            .takeIf { it.isNotEmpty() }
            ?.let { URI.create(it) }
            ?.also { client.endpointOverride(it) }

        val signer = when {
            useLegacyS3V4Signer -> S3Signer.LEGACY_V4
            else -> settings.signer
        }

        client.overrideConfiguration {
            it.retryPolicy { cfg ->
                cfg.backoffStrategy(ExponentialBackoffStrategy(Duration.ofSeconds(1), Duration.ofMinutes(1)))
                cfg.numRetries(5)
            }

            when (signer) {
                S3Signer.DEFAULT -> {}
                S3Signer.LEGACY_V4 -> it.putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create())
            }
        }

        val s3Client =
            when {
                settings.metadataCacheSettings?.enabled == true -> {
                    val cache: Cache<String, HeadObjectResponse> = Caffeine.newBuilder()
                        .maximumSize(settings.metadataCacheSettings.maximumSize)
                        .expireAfterWrite(Duration.ofSeconds(settings.metadataCacheSettings.expireAfterCreationSeconds))
                        .expireAfterAccess(Duration.ofSeconds(settings.metadataCacheSettings.expireAfterAccessSeconds))
                        .build()
                    CachableS3Client(client.build(), cache)
                }
                else -> client.build()
            }

        val keyPrefix = settings.resolveKeyPrefix(owner.repositoryName)
        val releaseNamespace = try {
            namespaceClaims.claim(
                owner = owner,
                endpoint = settings.endpoint,
                bucket = settings.bucketName,
                keyPrefix = keyPrefix,
            )
        } catch (exception: Exception) {
            s3Client.close()
            throw exception
        }

        return try {
            S3StorageProvider(
                failureFacade = failureFacade,
                s3 = s3Client,
                bucket = settings.bucketName,
                keyPrefix = keyPrefix,
                releaseNamespace = releaseNamespace,
            )
        } catch (exception: Exception) {
            releaseNamespace()
            s3Client.close()
            failureFacade.logger.error("Cannot connect to S3 storage provider: ${exception.message}")
            failureFacade.logger.error("S3 storage provider configuration:")
            failureFacade.logger.error("  - Bucket: ${settings.bucketName}")
            failureFacade.logger.error("  - Key prefix: ${keyPrefix.ifEmpty { "<none>" }}")
            failureFacade.logger.error("  - Region: ${region.id()} (isGlobalRegion: ${region.isGlobalRegion})")
            failureFacade.logger.error("  - Custom endpoint: $customEndpoint")
            failureFacade.logger.error("  - Path style access: $pathStyleAccessEnabled")
            failureFacade.logger.error("  - Signer: $signer")
            failureFacade.logger.error("  - Access key: ${maskSecret(settings.accessKey)}")
            failureFacade.logger.error("  - Secret key: ${maskSecret(settings.secretKey)}")
            throw IllegalStateException("Failed to initialize S3 storage provider", exception)
        }
    }

    override val settingsType: Class<S3StorageProviderSettings> =
        S3StorageProviderSettings::class.java

    override val type: String =
        "s3"

}

internal class S3NamespaceClaims {

    private data class Namespace(
        val endpoint: String,
        val bucket: String,
        val keyPrefix: String,
    ) {
        fun overlaps(other: Namespace): Boolean =
            endpoint == other.endpoint &&
                bucket == other.bucket &&
                (keyPrefix.startsWith(other.keyPrefix) || other.keyPrefix.startsWith(keyPrefix))
    }

    private data class Claim(
        val owner: StorageProviderOwner,
        val namespace: Namespace,
    )

    private val claims = mutableMapOf<Any, Claim>()

    fun claim(
        owner: StorageProviderOwner,
        endpoint: String,
        bucket: String,
        keyPrefix: String,
    ): () -> Unit =
        synchronized(claims) {
            val namespace = Namespace(
                endpoint = endpoint.trim().trimEnd('/'),
                bucket = bucket.trim(),
                keyPrefix = keyPrefix,
            )
            val conflict = claims.values.firstOrNull { claim ->
                claim.owner != owner && claim.namespace.overlaps(namespace)
            }

            require(conflict == null) {
                "S3 namespace for ${owner.providerId}:${owner.repositoryName} overlaps " +
                    "${conflict?.owner?.providerId}:${conflict?.owner?.repositoryName}"
            }

            val token = Any()
            claims[token] = Claim(owner, namespace)
            val release: () -> Unit = {
                synchronized(claims) {
                    claims.remove(token)
                }
            }
            release
        }
}
