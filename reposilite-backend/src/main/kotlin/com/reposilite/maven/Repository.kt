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
package com.reposilite.maven

import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.web.http.ErrorResponse
import org.apache.commons.codec.digest.DigestUtils
import panda.std.Result
import java.io.InputStream
import java.nio.file.attribute.FileTime

class Repository internal constructor(
    val name: String,
    val visibility: RepositoryVisibility,
    val redeployment: Boolean,
    val preserved: Int,
    val proxiedHosts: List<ProxiedHost>,
    private val storageProvider: StorageProvider,
) {

    init {
        if (name.length > REPOSITORY_NAME_MAX_LENGTH) {
            throw IllegalStateException("Repository name cannot exceed $REPOSITORY_NAME_MAX_LENGTH characters")
        }
    }

    @Suppress("unused")
    fun writeFileChecksums(location: Location, bytes: ByteArray): Result<Unit, ErrorResponse> {
        val md5 = location.resolveSibling(location.getSimpleName() + ".md5")
        val sha1 = location.resolveSibling(location.getSimpleName() + ".sha1")
        val sha256 = location.resolveSibling(location.getSimpleName() + ".sha256")
        val sha512 = location.resolveSibling(location.getSimpleName() + ".sha512")

        return putFile(md5, DigestUtils.md5(bytes).inputStream())
            .flatMap { putFile(sha1, DigestUtils.sha1(bytes).inputStream()) }
            .flatMap { putFile(sha256, DigestUtils.sha256(bytes).inputStream()) }
            .flatMap { putFile(sha512, DigestUtils.sha512(bytes).inputStream()) }
    }

    fun putFile(location: Location, inputStream: InputStream): Result<Unit, ErrorResponse> =
        storageProvider.putFile(location, inputStream)

    fun getFile(location: Location): Result<InputStream, ErrorResponse> =
        storageProvider.getFile(location)

    fun getFileDetails(location: Location): Result<out FileDetails, ErrorResponse> =
        storageProvider.getFileDetails(location)

    fun removeFile(location: Location): Result<Unit, ErrorResponse> =
        storageProvider.removeFile(location)

    fun getFiles(directoryLocation: Location): Result<List<Location>, ErrorResponse> =
        storageProvider.getFiles(directoryLocation)

    fun getLastModifiedTime(location: Location): Result<FileTime, ErrorResponse> =
        storageProvider.getLastModifiedTime(location)

    fun getFileSize(location: Location): Result<Long, ErrorResponse> =
        storageProvider.getFileSize(location)

    fun exists(location: Location): Boolean =
        storageProvider.exists(location)

    fun getUsage(): Result<Long, ErrorResponse> =
        storageProvider.usage()

    fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        storageProvider.canHold(contentLength)

    fun shutdown() =
        storageProvider.shutdown()

}
