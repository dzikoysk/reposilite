/*
 * Copyright (c) 2020 Dzikoysk
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
package org.panda_lang.reposilite.maven.repository

import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.maven.repository.api.RepositoryVisibility
import org.panda_lang.reposilite.maven.repository.api.RepositoryVisibility.PRIVATE
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
) : Comparator<Path?> {

    companion object {
        private val REPOSITORIES = Paths.get("repositories")
    }

    fun isPublic(): Boolean =
        !isPrivate()

    fun isPrivate(): Boolean =
        visibility == PRIVATE

    fun putFile(file: Path?, bytes: ByteArray?): Result<FileDetailsResponse, ErrorResponse> {
        return storageProvider.putFile(relativize(file)!!, bytes!!)
    }

    fun putFile(file: Path?, inputStream: InputStream?): Result<FileDetailsResponse, ErrorResponse> {
        return storageProvider.putFile(relativize(file)!!, inputStream!!)
    }

    fun getFile(file: Path?): Result<ByteArray, ErrorResponse> {
        return storageProvider.getFile(relativize(file)!!)
    }

    fun getFileDetails(file: Path?): Result<FileDetailsResponse, ErrorResponse> {
        return storageProvider.getFileDetails(relativize(file)!!)
    }

    fun removeFile(file: Path?): Result<Void, ErrorResponse> {
        return storageProvider.removeFile(relativize(file)!!)
    }

    fun getFiles(directory: Path?): Result<List<Path>, ErrorResponse> {
        return storageProvider.getFiles(relativize(directory)!!)
    }

    fun getLastModifiedTime(file: Path?): Result<FileTime, ErrorResponse> {
        return storageProvider.getLastModifiedTime(relativize(file)!!)
    }

    fun getFileSize(file: Path?): Result<Long, ErrorResponse> {
        return storageProvider.getFileSize(relativize(file)!!)
    }

    fun exists(file: Path?): Boolean {
        return storageProvider.exists(relativize(file)!!)
    }

    fun isDirectory(file: Path?): Boolean {
        return storageProvider.isDirectory(relativize(file)!!)
    }

    fun isFull(): Boolean =
        storageProvider.isFull()

    fun getUsage(): Long =
        storageProvider.usage()

    fun canHold(contentLength: Long): Boolean {
        return storageProvider.canHold(contentLength)
    }

    fun shutdown() {
        storageProvider.shutdown()
    }

    fun relativize(path: Path?): Path? {
        var path = path ?: return null

        if (!path.startsWith(REPOSITORIES)) {
            if (!path.startsWith(name)) {
                path = Paths.get(name).resolve(path)
            }
            path = REPOSITORIES.resolve(path)
        }
        else if (path.startsWith(name)) {
            path = REPOSITORIES.relativize(path)
        }

        return path
    }

    override fun compare(o1: Path?, o2: Path?): Int {
        return relativize(o1)!!.compareTo(relativize(o2))
    }

}