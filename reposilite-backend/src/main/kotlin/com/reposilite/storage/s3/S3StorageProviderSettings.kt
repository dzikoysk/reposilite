package com.reposilite.storage.s3

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.storage.StorageProviderSettings
import io.javalin.openapi.Custom

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
    val region: String = ""
) : StorageProviderSettings
