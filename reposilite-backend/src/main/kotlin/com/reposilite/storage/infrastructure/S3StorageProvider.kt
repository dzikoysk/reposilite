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
import com.reposilite.shared.FileType.DIRECTORY
import com.reposilite.shared.getExtension
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.safeResolve
import com.reposilite.storage.StorageProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.ContentType.Companion.OCTET_STREAM
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
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

    override fun putFile(file: Path, bytes: ByteArray): Result<DocumentInfo, ErrorResponse> =
        try {
            val builder = PutObjectRequest.builder()
            builder.bucket(bucket)
            builder.key(file.toString().replace('\\', '/'))
            builder.contentType(ContentType.getMimeTypeByExtension(file.getExtension()) ?: OCTET_STREAM)
            builder.contentLength(bytes.size.toLong())
            s3.putObject(builder.build(), RequestBody.fromBytes(bytes))

            DocumentInfo(
                file.getSimpleName(),
                ContentType.getContentTypeByExtension(file.getExtension()) ?: ContentType.APPLICATION_OCTET_STREAM,
                bytes.size.toLong(),
                { getFile(file).get() }
            ).asSuccess()
        }
        catch (exception: Exception) {
            logger.exception(exception)
            errorResponse(HttpCode.INTERNAL_SERVER_ERROR, "Failed to write $file")
        }

    override fun putFile(file: Path, inputStream: InputStream): Result<DocumentInfo, ErrorResponse> =
        try {
            val builder = PutObjectRequest.builder()
            builder.bucket(bucket)
            builder.key(file.toString().replace('\\', '/'))
            builder.contentType(ContentType.getMimeTypeByExtension(file.getExtension()) ?: OCTET_STREAM)

            val length = inputStream.available().toLong()
            builder.contentLength(length)

            s3.putObject(
                builder.build(),
                RequestBody.fromInputStream(inputStream, inputStream.available().toLong())
            )

            DocumentInfo(
                file.getSimpleName(),
                ContentType.getContentTypeByExtension(file.getExtension()) ?: APPLICATION_OCTET_STREAM,
                length,
                { inputStream }
            ).asSuccess()
        }
        catch (ioException: IOException) {
            logger.exception(ioException)
            errorResponse(HttpCode.INTERNAL_SERVER_ERROR, "Failed to write $file")
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
        if (file.toString() == "") { // why ""?
            getFiles(file)
                .map { it.map { path -> getFileDetails(path) } }
                .map { DirectoryInfo(file.fileName.toString(), it.map { result -> result.get() /* We should somehow flat map these errors maybe */}) }
        }
        else {
            head(file)?.let {
                DocumentInfo(
                    file.fileName.toString(),
                    ContentType.getContentTypeByExtension(file.getExtension()) ?: APPLICATION_OCTET_STREAM,
                    it.contentLength(),
                    { getFile(file).get() }
                ).asSuccess()
            }
            ?: errorResponse(NOT_FOUND, "File not found: $file")
        }

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

            val directoryString = directory.toString().replace('\\', '/')
            request.prefix(directoryString)
            //            request.delimiter("/");

            val response = s3.listObjects(request.build())
            val paths: MutableList<Path> = ArrayList()

            for (content in response.contents()) {
                val sub = content.key().substring(directoryString.length)
                paths.add(Paths.get(sub))
            }

            paths.asSuccess()
        }
        catch (exception: Exception) {
            errorResponse(HttpCode.INTERNAL_SERVER_ERROR, exception.localizedMessage)
        }

    override fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        head(file)
            ?.let { FileTime.from(it.lastModified()).asSuccess() }
            ?: getFiles(file)
                .map { files -> files.firstOrNull() }
                .mapErr { ErrorResponse(NOT_FOUND, "File not found: $file") }
                .flatMap { getLastModifiedTime(file.safeResolve(it!!.getName(0))) }

    override fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        head(file)
            ?.contentLength()
            ?.asSuccess()
            ?: errorResponse(NOT_FOUND, "File not found: $file")

    private fun head(file: Path): HeadObjectResponse? =
        try {
            val request = HeadObjectRequest.builder()
            request.bucket(bucket)
            request.key(file.toString().replace('\\', '/'))
            s3.headObject(request.build())
        }
        catch (ignored: NoSuchKeyException) {
            // ignored
            null
        }
        catch (exception: Exception) {
            exception.printStackTrace()
            null
        }

    override fun exists(file: Path): Boolean =
        head(file) != null

    override fun isDirectory(file: Path): Boolean =
        getFileDetails(file)
            .map { it.type == DIRECTORY }
            .orElseGet { false }
        /*
        with(getFile(file)) {
            isOk && get().available() > 0 && !exists(file)
        }
         */

    override fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun usage(): Result<Long, ErrorResponse> {
        TODO("Not yet implemented")
    }

    override fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        true.asSuccess()

    override fun getLogger(): Logger =
        journalist.logger

}