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

package com.reposilite.storage.s3.specification

import com.reposilite.storage.s3.S3StorageProviderFactory
import software.amazon.awssdk.services.s3.S3Client

internal abstract class S3StorageProviderFactorySpecification {

    private val factory = S3StorageProviderFactory()

    protected fun useClient(
        repositoryName: String,
        endpoint: String,
        bucket: String,
        keyPrefix: String,
        close: () -> Unit = {},
    ): S3Client =
        factory.registerClient(repositoryName, endpoint, bucket, keyPrefix, object : S3Client {
            override fun serviceName(): String =
                S3Client.SERVICE_NAME

            override fun close() =
                close.invoke()
        })

}
