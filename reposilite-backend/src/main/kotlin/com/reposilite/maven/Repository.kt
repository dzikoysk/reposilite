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
package com.reposilite.maven

import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.maven.api.RepositoryVisibility
import com.reposilite.shared.fs.FileDetails
import com.reposilite.shared.fs.getSimpleName
import com.reposilite.storage.StorageProvider
import com.reposilite.web.http.ErrorResponse
import org.apache.commons.codec.digest.DigestUtils
import panda.std.Result
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.attribute.FileTime

internal class Repository internal constructor(
    val name: String,
    val visibility: RepositoryVisibility,
    val redeployment: Boolean,
    val proxiedHosts: List<ProxiedHost>,
    private val storageProvider: StorageProvider,
) {

    init {
        if (name.length > REPOSITORY_NAME_MAX_LENGTH) {
            throw IllegalStateException("Repository name cannot exceed $REPOSITORY_NAME_MAX_LENGTH characters")
        }
    }

    @Suppress("unused")
    private fun writeFileChecksums(path: Path, bytes: ByteArray) {
        val md5 = path.resolveSibling(path.getSimpleName() + ".md5")
        putFile(md5, DigestUtils.md5(bytes).inputStream())

        val sha1 = path.resolveSibling(path.getSimpleName() + ".sha1")
        val sha256 = path.resolveSibling(path.getSimpleName() + ".sha256")
        val sha512 = path.resolveSibling(path.getSimpleName() + ".sha512")
        putFile(sha1, DigestUtils.sha1(bytes).inputStream())
        putFile(sha256, DigestUtils.sha256(bytes).inputStream())
        putFile(sha512, DigestUtils.sha512(bytes).inputStream())
    }

    fun putFile(file: Path, inputStream: InputStream): Result<Unit, ErrorResponse> =
        storageProvider.putFile(file, inputStream)

    fun getFile(file: Path): Result<InputStream, ErrorResponse> =
        storageProvider.getFile(file)

    fun getFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
        storageProvider.getFileDetails(file)

    fun removeFile(file: Path): Result<Unit, ErrorResponse> =
        storageProvider.removeFile(file)

    fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> =
        storageProvider.getFiles(directory)

    fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        storageProvider.getLastModifiedTime(file)

    fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        storageProvider.getFileSize(file)

    fun exists(file: Path): Boolean =
        storageProvider.exists(file)

    fun getUsage(): Result<Long, ErrorResponse> =
        storageProvider.usage()

    fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        storageProvider.canHold(contentLength)

    fun shutdown() =
        storageProvider.shutdown()

}