/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.REPOSITORY_NAME_MAX_LENGTH
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import org.apache.commons.codec.digest.DigestUtils
import panda.std.Result
import java.io.InputStream
import java.nio.file.attribute.FileTime

@Suppress("DeprecatedCallableAddReplaceWith")
class Repository internal constructor(
    val name: String,
    val visibility: RepositoryVisibility,
    val redeployment: Boolean,
    val preserveSnapshots: Boolean,
    val mirrorHosts: List<MirrorHost>,
    val storageProvider: StorageProvider,
) {

    init {
        check(name.length < REPOSITORY_NAME_MAX_LENGTH) { "Repository name cannot exceed $REPOSITORY_NAME_MAX_LENGTH characters" }
    }

    fun acceptsDeploymentOf(location: Location): Boolean =
        redeployment || location.getSimpleName().contains(METADATA_FILE) || !storageProvider.exists(location)

    fun writeFileChecksums(location: Location, bytes: ByteArray): Result<Unit, ErrorResponse> =
        writeFileChecksums(location, bytes.inputStream())

    fun writeFileChecksums(location: Location, bytes: InputStream): Result<Unit, ErrorResponse> {
        val md5 = location.resolveSibling(location.getSimpleName() + ".md5")
        val sha1 = location.resolveSibling(location.getSimpleName() + ".sha1")
        val sha256 = location.resolveSibling(location.getSimpleName() + ".sha256")
        val sha512 = location.resolveSibling(location.getSimpleName() + ".sha512")

        return storageProvider.putFile(md5, DigestUtils.md5Hex(bytes).byteInputStream())
            .flatMap { storageProvider.putFile(sha1, DigestUtils.sha1Hex(bytes).byteInputStream()) }
            .flatMap { storageProvider.putFile(sha256, DigestUtils.sha256Hex(bytes).byteInputStream()) }
            .flatMap { storageProvider.putFile(sha512, DigestUtils.sha512Hex(bytes).byteInputStream()) }
    }

    @Deprecated(message = "Use Repository#storageProvider")
    fun putFile(location: Location, inputStream: InputStream): Result<Unit, ErrorResponse> =
        storageProvider.putFile(location, inputStream)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getFile(location: Location): Result<InputStream, ErrorResponse> =
        storageProvider.getFile(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getFileDetails(location: Location): Result<out FileDetails, ErrorResponse> =
        storageProvider.getFileDetails(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun removeFile(location: Location): Result<Unit, ErrorResponse> =
        storageProvider.removeFile(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getFiles(directoryLocation: Location): Result<List<Location>, ErrorResponse> =
        storageProvider.getFiles(directoryLocation)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getLastModifiedTime(location: Location): Result<FileTime, ErrorResponse> =
        storageProvider.getLastModifiedTime(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getFileSize(location: Location): Result<Long, ErrorResponse> =
        storageProvider.getFileSize(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun exists(location: Location): Boolean =
        storageProvider.exists(location)

    @Deprecated(message = "Use Repository#storageProvider")
    fun getUsage(): Result<Long, ErrorResponse> =
        storageProvider.usage()

    @Deprecated(message = "Use Repository#storageProvider")
    fun canHold(contentLength: Long): Result<*, ErrorResponse> =
        storageProvider.canHold(contentLength)

    fun shutdown() =
        storageProvider.shutdown()

}
