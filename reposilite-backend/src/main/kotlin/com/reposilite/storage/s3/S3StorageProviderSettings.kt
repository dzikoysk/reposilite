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

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.storage.StorageProviderSettings
import io.javalin.openapi.Custom

@Doc(title = "S3 Metadata Cache", description = "Caches things like file size from S3 objects")
data class S3MetadataCacheSettings(
    @get:Doc(title = "Enabled", description = "Enable the cache?")
    val enabled: Boolean = false,
    @get:Doc(title = "Max Cache Entries", description = "Maximum number of cached entries")
    val maximumSize: Long = 10_000,
    @get:Doc(title = "Expire After Access", description = "Expire cache entries after the specified number of seconds since last read access")
    val expireAfterAccessSeconds: Long = 60 * 60 * 24, // 1 day
    @get:Doc(title = "Expire After Creation", description = "Expire cache entries after the specified number of seconds since the initial download")
    val expireAfterCreationSeconds: Long = 60 * 60 * 24, // 1 day
)


@Doc(title = "S3 Storage Provider", description = "Amazon S3 storage provider settings")
data class S3StorageProviderSettings(
    @get:Custom(name = "const", value = "s3")
    override val type: String = "s3",
    @get:Doc(title = "Bucket", description = "The selected AWS bucket")
    val bucketName: String = "",
    @get:Doc(title = "Endpoint", description = "Overwrite the AWS endpoint (optional)")
    val endpoint: String = "",
    @get:Doc(title = "Access Key", description = "Overwrite AWS access-key used to authenticate (optional)")
    val accessKey: String = "",
    @get:Doc(title = "Secret Key", description = "Overwrite AWS secret-key used to authenticate (optional)")
    val secretKey: String = "",
    @get:Doc(title = "Region", description = "Overwrite AWS region (optional)")
    val region: String = "",
    @get:Doc(title = "Key Prefix", description = "Optional prefix prepended to all object keys, e.g. to scope data within a bucket shared with other services")
    val prefix: String = "",
    @get:Doc(title = "Shared Bucket", description = "Namespace objects under the repository name so several repositories can share one bucket (optional)")
    val sharedBucket: Boolean = false,
    @get:Doc(title = "Local Metadata Cache", description = "Local metadata cache settings (optional). The default is no caching. NOTE: This cache is local only. If you run multiple instances, they will not share the cache!")
    val metadataCacheSettings: S3MetadataCacheSettings? = null,
) : StorageProviderSettings

fun S3StorageProviderSettings.resolveKeyPrefix(repositoryName: String): String {
    val base = prefix.trim().trim('/').let { if (it.isEmpty()) "" else "$it/" }
    return when {
        sharedBucket -> base + repositoryName.trim().trim('/') + "/"
        else -> base
    }
}

fun findS3SharedBucketConflicts(repositories: List<Pair<String, S3StorageProviderSettings>>): Set<String> {
    val conflicts = mutableSetOf<String>()

    repositories
        .groupBy { (_, settings) -> settings.endpoint.trim().trimEnd('/') to settings.bucketName.trim() }
        .values
        .filter { it.size > 1 }
        .forEach { group ->
            val keyPrefixes = group.map { (id, settings) -> id to settings.resolveKeyPrefix(id) }
            keyPrefixes.forEach { (id, keyPrefix) ->
                // collide when key namespaces are equal or one nests under the other
                val collides = keyPrefixes.any { (otherId, otherKeyPrefix) ->
                    otherId != id && (keyPrefix.startsWith(otherKeyPrefix) || otherKeyPrefix.startsWith(keyPrefix))
                }
                if (collides) conflicts += id
            }
        }

    return conflicts
}
