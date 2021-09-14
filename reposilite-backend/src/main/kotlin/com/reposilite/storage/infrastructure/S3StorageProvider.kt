/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.storage.infrastructure

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.DirectoryInfo
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.SimpleDirectoryInfo
import com.reposilite.shared.FileType.DIRECTORY
import com.reposilite.shared.getExtension
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.safeResolve
import com.reposilite.storage.StorageProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.transferLargeTo
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.ContentType.Companion.OCTET_STREAM
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime

internal class S3StorageProvider(
    private val journalist: Journalist,
    private val s3: S3Client,
    private val bucket: String,
) : StorageProvider, Journalist {

    init {
        try {
            s3.createBucket {
                it.bucket(bucket)
            }
        } catch (bucketExists: BucketAlreadyExistsException) {
            // ignored
        }
    }

    override fun putFile(file: Path, inputStream: InputStream): Result<DocumentInfo, ErrorResponse> =
        try {
            val builder = PutObjectRequest.builder()
            builder.bucket(bucket)
            builder.key(file.toString().replace('\\', '/'))
            builder.contentType(ContentType.getMimeTypeByExtension(file.getExtension()) ?: OCTET_STREAM)

            // So... S3 API is disabled and requires content-length of every inserted object.
            // It means we're pretty doomed if we'd like to re-stream large artifacts that size is unknown.
            // To avoid 'Out of Memory' scenario of in-memory rewrite by S3 client, it's just safer to redistribute content from temp file.
            // Few unresolved issues addressing this case:
            // ~ https://github.com/aws/aws-sdk-java/issues/474
            // ~ https://github.com/aws/aws-sdk-java-v2/issues/37
            val tempFile = File.createTempFile("reposilite-", "-deploy")
            inputStream.transferLargeTo(tempFile.outputStream())
            val contentLength = tempFile.length()
            builder.contentLength(contentLength)

            s3.putObject(builder.build(), RequestBody.fromFile(tempFile))
            tempFile.delete()

            DocumentInfo(
                file.getSimpleName(),
                ContentType.getContentTypeByExtension(file.getExtension()) ?: APPLICATION_OCTET_STREAM,
                contentLength,
                { inputStream }
            ).asSuccess()
        }
        catch (ioException: IOException) {
            logger.exception(ioException)
            errorResponse(INTERNAL_SERVER_ERROR, "Failed to write $file")
        }

    override fun getFile(file: Path): Result<InputStream, ErrorResponse> =
        try {
            val request = GetObjectRequest.builder()
            request.bucket(bucket)
            request.key(file.toString().replace('\\', '/'))

            s3.getObject(request.build()).asSuccess()
            // val bytes = ByteArray(Math.toIntExact(response.response().contentLength()))
            // response.read(bytes)
        }
        catch (noSuchKeyException: NoSuchKeyException) {
            errorResponse(NOT_FOUND, "File not found: $file")
        }
        catch (ioException: IOException) {
            errorResponse(NOT_FOUND, "File not found: $file")
        }

    override fun getFileDetails(file: Path): Result<FileDetails, ErrorResponse> =
        head(file)
            .map { toDocumentInfo(file, it) }
            .flatMapErr { getFiles(file).map { toDirectoryInfo(file, it) } }

    private fun getSimplifiedFileDetails(file: Path): FileDetails =
        head(file)
            .fold(
                { toDocumentInfo(file, it) },
                { SimpleDirectoryInfo(file.getSimpleName()) }
            )

    private fun toDocumentInfo(file: Path, head: HeadObjectResponse): FileDetails =
        DocumentInfo(
            file.getSimpleName(),
            ContentType.getContentTypeByExtension(file.getExtension()) ?: APPLICATION_OCTET_STREAM,
            head.contentLength(),
            { getFile(file).get() }
        )

    private fun toDirectoryInfo(file: Path, files: List<Path>): DirectoryInfo =
        DirectoryInfo(
            file.getSimpleName(),
            files.map { getSimplifiedFileDetails(it) }
        )

    override fun removeFile(file: Path): Result<*, ErrorResponse> =
        with(DeleteObjectRequest.builder()) {
            bucket(bucket)
            key(file.toString().replace('\\', '/'))
            s3.deleteObject(build())
        }.asSuccess()

    override fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> =
        try {
            val request = ListObjectsRequest.builder()
            request.bucket(bucket)

            val directoryString =directory.toString().replace('\\', '/')
            request.prefix(directoryString)

            val paths = s3.listObjects(request.build())
                .contents().asSequence()
                // .map { it.key().substring(directoryString.length) }
                // .map { it.split('/', limit = 2).firstOrNull() ?: it }
                // .map { Paths.get(it) }
                .map { Paths.get(it.key()) }
                .toList()

            if (paths.isEmpty()) {
                errorResponse(NOT_FOUND, "Directory not found or is empty")
            }
            else {
                paths.asSuccess()
            }
        }
        catch (exception: Exception) {
            errorResponse(INTERNAL_SERVER_ERROR, exception.localizedMessage)
        }

    override fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        head(file)
            .map { FileTime.from(it.lastModified()) }
            .flatMapErr {
                getFiles(file)
                    .map { files -> files.firstOrNull() }
                    .mapErr { ErrorResponse(NOT_FOUND, "File not found: $file") }
                    .flatMap { getLastModifiedTime(file.safeResolve(it!!.getName(0))) }
            }

    override fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        head(file)
            .map { it.contentLength() }

    private fun head(file: Path): Result<HeadObjectResponse, ErrorResponse> =
        try {
            val request = HeadObjectRequest.builder()
            request.bucket(bucket)
            request.key(file.toString().replace('\\', '/'))
            s3.headObject(request.build()).asSuccess()
        }
        catch (ignored: NoSuchKeyException) {
            errorResponse(NOT_FOUND, ignored.message ?: "File not found")
        }
        catch (exception: Exception) {
            errorResponse(INTERNAL_SERVER_ERROR, exception.message ?: "Generic exception")
        }

    override fun exists(file: Path): Boolean =
        head(file).fold(
            { true },
            { getFiles(file).isOk }
        )

    // TOFIX: Provide simplified version based on metadata, maybe reverse it with isDocument based on head
    override fun isDirectory(file: Path): Boolean =
        getFileDetails(file)
            .map { it.type == DIRECTORY }
            .orElseGet { false }
        /*
        with(getFile(file)) {
            isOk && get().available() > 0 && !exists(file)
        }
         */

    override fun usage(): Result<Long, ErrorResponse> =
        ok(-1)

    override fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        ok(true)

    override fun getLogger(): Logger =
        journalist.logger

}