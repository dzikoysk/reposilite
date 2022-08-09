/*
 * Copyright (c) 2022 dzikoysk
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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.SimpleDirectoryInfo
import com.reposilite.storage.api.toLocation
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.internalServerError
import com.reposilite.web.http.notFoundError
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.ContentType.Companion.OCTET_STREAM
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess
import panda.std.letIf
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.attribute.FileTime

class S3StorageProvider(
    private val failureFacade: FailureFacade,
    private val s3: S3Client,
    private val bucket: String,
) : StorageProvider, Journalist {

    init {
        val headBucketRequest = HeadBucketRequest.builder().bucket(bucket).build()

        try {
            s3.headBucket(headBucketRequest)
        } catch (noSuchBucket: NoSuchBucketException) {
            try {
                s3.createBucket { it.bucket(bucket) }
            } catch (bucketExists: BucketAlreadyExistsException) {
                // ignored
            } catch (buckedOwned: BucketAlreadyOwnedByYouException) {
                // ignored
            }
        }
    }

    override fun putFile(location: Location, inputStream: InputStream): Result<Unit, ErrorResponse> =
        inputStream.use { data ->
            // So... S3 API is disabled and requires content-length of every inserted object.
            // It means we're pretty doomed if we'd like to re-stream large artifacts that size is unknown.
            // To avoid 'Out of Memory' scenario of in-memory rewrite by S3 client, it's just safer to redistribute content from temp file.
            // Few unresolved issues addressing this case:
            // ~ https://github.com/aws/aws-sdk-java/issues/474
            // ~ https://github.com/aws/aws-sdk-java-v2/issues/37
            val temporary = File.createTempFile("reposilite-", "-s3-put")

            temporary.outputStream().use { destination ->
                data.copyTo(destination)
            }

            try {
                val builder = PutObjectRequest.builder()
                builder.bucket(bucket)
                builder.key(location.toString().replace('\\', '/'))
                builder.contentType(ContentType.getMimeTypeByExtension(location.getExtension()) ?: OCTET_STREAM)
                builder.contentLength(temporary.length())
                s3.putObject(builder.build(), RequestBody.fromFile(temporary))
                ok(Unit)
            } catch (ioException: IOException) {
                failureFacade.throwException("S3 Storage Provider failed with IO Exception", ioException)
                internalServerError("Failed to write $location")
            } finally {
                temporary.delete()
            }
        }

    override fun getFile(location: Location): Result<InputStream, ErrorResponse> =
        try {
            val request = GetObjectRequest.builder()
            request.bucket(bucket)
            request.key(location.toString().replace('\\', '/'))
            s3.getObject(request.build()).asSuccess()
            // val bytes = ByteArray(Math.toIntExact(response.response().contentLength()))
            // response.read(bytes)
        } catch (noSuchKeyException: NoSuchKeyException) {
            notFoundError("File not found: $location")
        } catch (ioException: IOException) {
            notFoundError("File not found: $location")
        }

    override fun getFileDetails(location: Location): Result<FileDetails, ErrorResponse> =
        head(location)
            .map { toDocumentInfo(location, it) }
            .flatMapErr { getFiles(location).map { toDirectoryInfo(location, it) } }

    private fun getSimplifiedFileDetails(location: Location): FileDetails =
        head(location)
            .fold(
                { toDocumentInfo(location, it) },
                { SimpleDirectoryInfo(location.getSimpleName()) }
            )

    private fun toDocumentInfo(location: Location, head: HeadObjectResponse): FileDetails =
        DocumentInfo(
            location.getSimpleName(),
            ContentType.getContentTypeByExtension(location.getExtension()) ?: APPLICATION_OCTET_STREAM,
            head.contentLength()
        )

    private fun toDirectoryInfo(location: Location, files: List<Location>): DirectoryInfo =
        DirectoryInfo(
            location.getSimpleName(),
            files.map { getSimplifiedFileDetails(it) }
        )

    override fun removeFile(location: Location): Result<Unit, ErrorResponse> =
        try {
            val request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(location.toString().replace('\\', '/'))
                .build()

            s3.listObjectsV2(request)
                .contents()
                .asSequence()
                .map { createDeleteRequest(it.key().toLocation()) }
                .forEach { s3.deleteObject(it) }

            ok(Unit)
        } catch (exception: Exception) {
            internalServerError(exception.message ?: exception::class.toString())
        }

    private fun createDeleteRequest(location: Location): DeleteObjectRequest =
        DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(location.toString().replace('\\', '/'))
            .build()

    override fun getFiles(location: Location): Result<List<Location>, ErrorResponse> =
        try {
            val directoryString = location.toString().replace('\\', '/')
                .let { "$it/" }
                .letIf({ it == "/" }, { "" })

            val request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(directoryString)
                .delimiter("/")

            val directories = s3.listObjectsV2(request.build())
                .commonPrefixes()
                .asSequence()
                .map { it.prefix().toLocation() }
                .toList()

            val files = s3.listObjectsV2(request.build())
                .contents()
                .asSequence()
                .map { it.key().toLocation() }
                .toList()

            val paths = directories + files

            if (paths.isEmpty())
                notFoundError("Directory not found or is empty")
            else
                paths.asSuccess()
        } catch (exception: Exception) {
            internalServerError(exception.localizedMessage)
        }

    override fun getLastModifiedTime(location: Location): Result<FileTime, ErrorResponse> =
        head(location)
            .map { FileTime.from(it.lastModified()) }

    override fun getFileSize(location: Location): Result<Long, ErrorResponse> =
        head(location)
            .map { it.contentLength() }

    private fun head(location: Location): Result<HeadObjectResponse, ErrorResponse> =
        try {
            val request = HeadObjectRequest.builder()
            request.bucket(bucket)
            request.key(location.toString().replace('\\', '/'))
            s3.headObject(request.build()).asSuccess()
        } catch (ignored: NoSuchKeyException) {
            notFoundError(ignored.message ?: "File not found")
        } catch (exception: Exception) {
            internalServerError(exception.message ?: "Generic exception")
        }

    override fun exists(location: Location): Boolean =
        head(location)
            .fold(
                { true },
                { getFiles(location).isOk }
            )

    override fun usage(): Result<Long, ErrorResponse> =
        ok(-1)

    override fun canHold(contentLength: Long): Result<Long, ErrorResponse> =
        ok(Long.MAX_VALUE)

    override fun getLogger(): Logger =
        failureFacade.logger

}
