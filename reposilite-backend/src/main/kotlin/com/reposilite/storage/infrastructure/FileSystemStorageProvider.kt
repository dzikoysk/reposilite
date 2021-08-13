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

import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.toDocumentInfo
import com.reposilite.maven.api.toFileDetails
import com.reposilite.shared.FileType.DIRECTORY
import com.reposilite.shared.catchIOException
import com.reposilite.shared.delete
import com.reposilite.shared.exists
import com.reposilite.shared.getLastModifiedTime
import com.reposilite.shared.inputStream
import com.reposilite.shared.listFiles
import com.reposilite.shared.size
import com.reposilite.shared.type
import com.reposilite.storage.StorageProvider
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode
import panda.std.Result
import panda.std.function.ThrowingBiFunction
import panda.std.function.ThrowingFunction
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.FileTime

/**
 * @param rootDirectory root directory of storage space
 */
internal abstract class FileSystemStorageProvider protected constructor(
    private val rootDirectory: Path
 ) : StorageProvider {

    override fun putFile(file: Path, bytes: ByteArray): Result<DocumentInfo, ErrorResponse> =
        putFile(file, bytes, { it.size }, { input, output -> output.write(ByteBuffer.wrap(input)) })

    override fun putFile(file: Path, inputStream: InputStream): Result<DocumentInfo, ErrorResponse> =
        putFile(file, inputStream, { it.available() }) { input, output ->
            val buffer = ByteArrayOutputStream()
            var nRead: Int
            val data = ByteArray(1024)

            while (input.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }

            buffer.flush()
            val byteArray = buffer.toByteArray()
            output.write(ByteBuffer.wrap(byteArray))
            byteArray.size
        }

    private fun <T> putFile(
        file: Path,
        input: T,
        measure: ThrowingFunction<T, Int, IOException>,
        writer: ThrowingBiFunction<T, FileChannel, Int, IOException>
    ): Result<DocumentInfo, ErrorResponse> =
        catchIOException {
            resolved(file)
                .let { file ->
                    val size = measure.apply(input).toLong()

                    if (canHold(size).isErr) {
                        return@catchIOException errorResponse(HttpCode.INSUFFICIENT_STORAGE, "Not enough storage space available")
                    }

                    if (file.parent != null && !Files.exists(file.parent)) {
                        Files.createDirectories(file.parent)
                    }

                    if (!Files.exists(file)) {
                        Files.createFile(file)
                    }

                    val fileChannel = FileChannel.open(file, WRITE, CREATE)
                    // TOFIX: FS locks are not truly respected, there might be a need to enhanced it with .lock file to be sure if it's respected.
                    // In theory people should not really share the same FS through instances.
                    // ~ https://github.com/dzikoysk/reposilite/issues/264
                    fileChannel.lock()

                    /*val bytesWritten =*/ writer.apply(input, fileChannel).toLong() // do we need this result
                    fileChannel.close()

                    toDocumentInfo(file)
                }
        }

    override fun getFile(file: Path): Result<InputStream, ErrorResponse> =
        resolved(file).inputStream()

    override fun getFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
        toFileDetails(resolved(file))

    override fun removeFile(file: Path): Result<*, ErrorResponse> =
        resolved(file).delete()

    override fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> =
        resolved(directory).listFiles()

    override fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> =
        resolved(file).getLastModifiedTime()

    override fun getFileSize(file: Path): Result<Long, ErrorResponse> =
        resolved(file).size()

    override fun exists(file: Path): Boolean =
        resolved(file).exists().isOk

    override fun isDirectory(file: Path): Boolean =
        resolved(file).type() == DIRECTORY

    override fun usage(): Result<Long, ErrorResponse> =
        rootDirectory.size()

    override fun isFull(): Boolean =
        canHold(0).isErr

    override fun shutdown() {}

    private fun resolved(file: Path): Path =
        rootDirectory.resolve(file)

}