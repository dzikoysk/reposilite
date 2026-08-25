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
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.DelegatingS3Client
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.nio.file.Path

/**
 * This S3 clients caches head object requests
 */
class CachableS3Client : DelegatingS3Client {
    private val delegate : S3Client
    private val cache : Cache<String, HeadObjectResponse>

    constructor(s3: S3Client, headObjectCache: Cache<String, HeadObjectResponse>) : super(s3) {
        this.delegate = s3
        this.cache = headObjectCache
    }

    override fun headObject(headObjectRequest: HeadObjectRequest): HeadObjectResponse {
        return cache.get(headObjectRequest.key(), { _ -> delegate.headObject(headObjectRequest) })
    }

    override fun deleteObject(deleteObjectRequest: DeleteObjectRequest): DeleteObjectResponse? {
        val response = super.deleteObject(deleteObjectRequest)
        invalidate(deleteObjectRequest.key())
        return response
    }

    override fun putObject(putObjectRequest: PutObjectRequest, requestBody: RequestBody?): PutObjectResponse? {
        val response = super.putObject(putObjectRequest, requestBody)
        invalidate(putObjectRequest.key())
        return response
    }

    private fun invalidate(s3Key: String) {
        cache.invalidate(s3Key)
        // If a new object has been written, we also want to invalidate the cache for its maven-metadata.xml file.
        // We don't look for special suffix, we just go blindly one level up and invalidate the maven-metadata.xml file.
        // When in doubt, we invalidate the cache a bit too often, but it keeps the logic simple and more robust
        Path.of(s3Key).parent?.parent?.let { parentPath ->
            val metadataKey = parentPath.resolve("maven-metadata.xml").toString()
            cache.asMap().keys.removeIf { s -> s.startsWith(metadataKey) }
        }
    }
}

