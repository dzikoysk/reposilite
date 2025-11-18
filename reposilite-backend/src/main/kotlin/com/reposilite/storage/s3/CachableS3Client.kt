package com.reposilite.storage.s3

import com.github.benmanes.caffeine.cache.Cache
import software.amazon.awssdk.services.s3.DelegatingS3Client
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse

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
        cache.invalidate(deleteObjectRequest.key())
        return response
    }
}

