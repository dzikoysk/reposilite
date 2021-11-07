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

import com.reposilite.shared.fs.FileDetails
import com.reposilite.shared.fs.catchIOException
import com.reposilite.shared.fs.delete
import com.reposilite.shared.fs.exists
import com.reposilite.shared.fs.getLastModifiedTime
import com.reposilite.shared.fs.inputStream
import com.reposilite.shared.fs.listFiles
import com.reposilite.shared.fs.safeResolve
import com.reposilite.shared.fs.size
import com.reposilite.shared.fs.toFileDetails
import com.reposilite.storage.StorageProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.INSUFFICIENT_STORAGE
import panda.std.Result
import panda.std.Result.ok
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.FileTime

/**
 * @param rootDirectory root directory of storage space
 */
internal abstract class FileSystemStorageProvider protected constructor(
    private val rootDirectory: Path
 ) : StorageProvider {

    override fun putFile(path: Path, inputStream: InputStream): Result<Unit, ErrorResponse> =
        catchIOException {
            inputStream.use { data ->
                val spaceResponse = canHold(data.available().toLong()) // we don't really care about edge scenarios, so we don't really need precise quotas

                if (spaceResponse.isErr) {
                    return@catchIOException errorResponse(INSUFFICIENT_STORAGE, "Not enough storage space available: ${spaceResponse.error.message}")
                }

                val file = resolved(path)

                if (file.parent != null && !Files.exists(file.parent)) {
                    Files.createDirectories(file.parent)
                }

                // TOFIX: FS locks are not truly respected, there might be a need to enhanced it with .lock file to be sure if it's respected.
                // In theory people shouldn't redeploy multiple times the same file, but who knows.
                // Let's try with temporary files.
                // ~ https://github.com/dzikoysk/reposilite/issues/264

                val temporaryFile = File.createTempFile("reposilite-", "-fs-put")

                temporaryFile.outputStream().use { destination ->
                    data.copyTo(destination)
                }

                Files.move(temporaryFile.toPath(), file, REPLACE_EXISTING)
                ok(Unit)
            }
        }

    override fun getFile(path: Path): Result<InputStream, ErrorResponse> =
        resolved(path).inputStream()

    override fun getFileDetails(path: Path): Result<out FileDetails, ErrorResponse> =
        toFileDetails(resolved(path))

    override fun removeFile(path: Path): Result<Unit, ErrorResponse> =
        resolved(path).delete()

    override fun getFiles(path: Path): Result<List<Path>, ErrorResponse> =
        resolved(path).listFiles()

    override fun getLastModifiedTime(path: Path): Result<FileTime, ErrorResponse> =
        resolved(path).getLastModifiedTime()

    override fun getFileSize(path: Path): Result<Long, ErrorResponse> =
        resolved(path).size()

    override fun exists(file: Path): Boolean =
        resolved(file).exists().isOk

    override fun usage(): Result<Long, ErrorResponse> =
        rootDirectory.size()

    override fun shutdown() {}

    private fun resolved(file: Path): Path =
        rootDirectory.safeResolve(file)

}