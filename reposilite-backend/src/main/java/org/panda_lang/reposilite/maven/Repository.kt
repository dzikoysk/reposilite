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
package org.panda_lang.reposilite.maven

import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.api.FileDetailsResponse
import org.panda_lang.reposilite.maven.api.RepositoryVisibility
import org.panda_lang.reposilite.maven.api.RepositoryVisibility.PRIVATE
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.utilities.commons.function.Result
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime

class Repository internal constructor(
    val name: String,
    private val visibility: RepositoryVisibility,
    private val storageProvider: StorageProvider,
    val isDeployEnabled: Boolean
) : Comparator<Path> {

    companion object {
        private val REPOSITORIES = Paths.get("repositories")
    }

    override fun compare(path: Path, toPath: Path): Int =
        relativize(path).compareTo(relativize(toPath))

    fun isPublic(): Boolean =
        !isPrivate()

    fun isPrivate(): Boolean =
        visibility == PRIVATE

    fun putFile(file: Path, bytes: ByteArray): Result<FileDetailsResponse, ErrorResponse> =
        storageProvider.putFile(relativize(file), bytes)

    fun putFile(file: Path, inputStream: InputStream): Result<FileDetailsResponse, ErrorResponse> =
        storageProvider.putFile(relativize(file), inputStream)

    fun getFile(file: Path): Result<ByteArray, ErrorResponse> =
        storageProvider.getFile(relativize(file))

    fun getFileDetails(file: Path): Result<FileDetailsResponse, ErrorResponse> =
        storageProvider.getFileDetails(relativize(file))

    fun removeFile(file: Path): Result<Void, ErrorResponse> =
        storageProvider.removeFile(relativize(file))

    fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> =
        storageProvider.getFiles(relativize(directory))

    fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        storageProvider.getLastModifiedTime(relativize(file))

    fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        storageProvider.getFileSize(relativize(file))

    fun exists(file: Path): Boolean =
        storageProvider.exists(relativize(file))

    fun isDirectory(file: Path): Boolean =
        storageProvider.isDirectory(relativize(file))

    fun isFull(): Boolean =
        storageProvider.isFull()

    fun getUsage(): Long =
        storageProvider.usage()

    fun canHold(contentLength: Long): Boolean =
        storageProvider.canHold(contentLength)

    fun shutdown() =
        storageProvider.shutdown()

    fun relativize(path: Path): Path {
        var relativePath = path

        if (!relativePath.startsWith(REPOSITORIES)) {
            if (!relativePath.startsWith(name)) {
                relativePath = Paths.get(name).resolve(relativePath)
            }

            relativePath = REPOSITORIES.resolve(relativePath)
        }
        else if (relativePath.startsWith(name)) {
            relativePath = REPOSITORIES.relativize(relativePath)
        }

        return relativePath
    }

}