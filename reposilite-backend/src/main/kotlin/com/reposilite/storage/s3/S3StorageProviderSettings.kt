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
    @get:Doc(title = "Metadata Cache", description = "Set metadata cache settings (optional) - default is disabling caching")
    val metadataCacheSettings: S3MetadataCacheSettings? = null,
) : StorageProviderSettings
