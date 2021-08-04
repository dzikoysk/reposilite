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
package com.reposilite.maven.api

import com.reposilite.failure.api.ErrorResponse
import com.reposilite.storage.StorageProvider
import panda.std.Result
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.attribute.FileTime

enum class RepositoryVisibility {
    PUBLIC,
    HIDDEN,
    PRIVATE
}

class Repository internal constructor(
    val name: String,
    val visibility: RepositoryVisibility,
    private val storageProvider: StorageProvider,
    val isDeployEnabled: Boolean
) {

    fun putFile(file: Path, bytes: ByteArray): Result<DocumentInfo, ErrorResponse> =
        storageProvider.putFile(file, bytes)

    fun putFile(file: Path, inputStream: InputStream): Result<DocumentInfo, ErrorResponse> =
        storageProvider.putFile(file, inputStream)

    fun getFile(file: Path): Result<InputStream, ErrorResponse> =
        storageProvider.getFile(file)

    fun getFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
        storageProvider.getFileDetails(file)

    fun removeFile(file: Path): Result<*, ErrorResponse> =
        storageProvider.removeFile(file)

    fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> =
        storageProvider.getFiles(directory)

    fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        storageProvider.getLastModifiedTime(file)

    fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        storageProvider.getFileSize(file)

    fun exists(file: Path): Boolean =
        storageProvider.exists(file)

    fun isDirectory(file: Path): Boolean =
        storageProvider.isDirectory(file)

    fun isFull(): Boolean =
        storageProvider.isFull()

    fun getUsage(): Result<Long, ErrorResponse> =
        storageProvider.usage()

    fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        storageProvider.canHold(contentLength)

    fun shutdown() =
        storageProvider.shutdown()

}