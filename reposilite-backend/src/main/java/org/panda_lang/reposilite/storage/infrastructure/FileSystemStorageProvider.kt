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

package org.panda_lang.reposilite.storage.infrastructure

import org.apache.http.HttpStatus
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.api.FileDetailsResponse
import org.panda_lang.reposilite.shared.utils.FilesUtils
import org.panda_lang.reposilite.shared.utils.FilesUtils.getMimeType
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.utilities.commons.function.Result
import org.panda_lang.utilities.commons.function.ThrowingBiFunction
import org.panda_lang.utilities.commons.function.ThrowingFunction
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
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

/**
 * @param rootDirectory root directory of storage space
 */
internal abstract class FileSystemStorageProvider private constructor(
    protected val rootDirectory: Path
 ) : StorageProvider {


    companion object {
        fun of(rootDirectory: Path, quota: String): FileSystemStorageProvider {
            return if (quota.endsWith("%")) {
                of(rootDirectory, quota.substring(0, quota.length - 1).toInt() / 100.0)
            } else {
                of(rootDirectory, FilesUtils.displaySizeToBytesCount(quota))
            }
        }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        fun of(rootDirectory: Path, maxSize: Long): FileSystemStorageProvider {
            return FixedQuota(rootDirectory, maxSize)
        }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        fun of(rootDirectory: Path, maxPercentage: Double): FileSystemStorageProvider {
            return PercentageQuota(rootDirectory, maxPercentage)
        }
    }

    override fun putFile(file: Path, bytes: ByteArray): Result<FileDetailsResponse, ErrorResponse> {
        return this.putFile(file, bytes, { it.size }) { input, output ->
            output.write(ByteBuffer.wrap(input))
        }
    }

    override fun putFile(file: Path, inputStream: InputStream): Result<FileDetailsResponse, ErrorResponse> {
        return this.putFile(file, inputStream, { it.available() }) { input, output ->
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
    }

    private fun <T> putFile(
        file: Path,
        input: T,
        measure: ThrowingFunction<T, Int, IOException>,
        writer: ThrowingBiFunction<T, FileChannel, Int, IOException>
    ): Result<FileDetailsResponse, ErrorResponse> {
        return try {
            val size = measure.apply(input).toLong()

            if (!canHold(size)) {
                return Result.error(ErrorResponse(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available"))
            }

            if (file.parent != null && !Files.exists(file.parent)) {
                Files.createDirectories(file.parent)
            }

            if (!Files.exists(file)) {
                Files.createFile(file)
            }

            val fileChannel = FileChannel.open(file, WRITE, CREATE)
            fileChannel.lock()

            val bytesWritten = writer.apply(input, fileChannel).toLong()
            fileChannel.close()

            Result.ok(
                FileDetailsResponse(
                    FileDetailsResponse.FILE,
                    file.fileName.toString(),
                    FileDetailsResponse.DATE_FORMAT.format(LocalDate.now()),
                    getMimeType(file.toString(), "application/octet-stream"),
                    bytesWritten
                )
            )
        }
        catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun getFile(file: Path): Result<ByteArray, ErrorResponse> {
        return if (!Files.exists(file) || Files.isDirectory(file)) {
            Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
        }
        else try {
            Result.ok(Files.readAllBytes(file))
        }
        catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun getFileDetails(file: Path): Result<FileDetailsResponse, ErrorResponse> {
        return if (!Files.exists(file)) {
            Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
        }
        else try {
            Result.ok(
                FileDetailsResponse(
                    if (Files.isDirectory(file)) FileDetailsResponse.DIRECTORY else FileDetailsResponse.FILE,
                    file.fileName.toString(),
                    FileDetailsResponse.DATE_FORMAT.format(Files.getLastModifiedTime(file).toInstant()), // TOFIX: Verify if #toInstant() is the best way to do this
                    getMimeType(file.fileName.toString(), "application/octet-stream"),
                    Files.size(file)
                )
            )
        } catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun removeFile(file: Path): Result<Void, ErrorResponse> {
        return try {
            if (!Files.exists(file)) {
                return Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
            }

            Files.delete(file)
            Result.ok(null)
        } catch (ioException: IOException) {
            Result.error(ErrorResponse(500, ioException.localizedMessage))
        }
    }

    override fun getFiles(directory: Path): Result<List<Path>, ErrorResponse> {
        return try {
            Result.ok(Files.walk(directory, 1).filter { path: Path -> path != directory }
                .collect(Collectors.toList()))
        } catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun getLastModifiedTime(file: Path): Result<FileTime, ErrorResponse> {
        return try {
            if (!Files.exists(file)) {
                Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
            } else Result.ok(Files.getLastModifiedTime(file))
        } catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun getFileSize(file: Path): Result<Long, ErrorResponse> {
        return try {
            if (!Files.exists(file)) {
                return Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "File not found: $file"))
            }

            var size: Long = 0

            if (Files.isDirectory(file)) {
                for (path in Files.walk(file).collect(Collectors.toList())) {
                    size += Files.size(path)
                }
            }
            else {
                size = Files.size(file)
            }

            Result.ok(size)
        }
        catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ioException.localizedMessage))
        }
    }

    override fun exists(file: Path): Boolean {
        return Files.exists(file) && !Files.isDirectory(file)
    }

    override fun isDirectory(file: Path): Boolean {
        return Files.isDirectory(file)
    }

    override fun usage(): Long {
            val usage = AtomicLong()
            try {
                Files.walk(rootDirectory).forEach { path: Path? ->
                    if (Files.exists(path) && !Files.isDirectory(path)) {
                        try {
                            usage.set(usage.get() + Files.size(path))
                        } catch (ignored: IOException) {
                        }
                    }
                }
            } catch (e: IOException) {
                usage.set(-1L)
            }
            return usage.get()
        }

    override fun shutdown() {}

    private class FixedQuota(rootDirectory: Path, private val maxSize: Long) : FileSystemStorageProvider(rootDirectory) {

        override fun isFull(): Boolean {
            return !canHold(0)
        }

        override fun canHold(contentLength: Long): Boolean {
            return usage() + contentLength < maxSize
        }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        init {
            if (maxSize <= 0) {
                throw RuntimeException()
            }
        }
    }

    private class PercentageQuota(rootDirectory: Path, private val maxPercentage: Double) : FileSystemStorageProvider(rootDirectory) {
        override fun isFull(): Boolean {
            return !canHold(0)
        }

        override fun canHold(contentLength: Long): Boolean {
            try {
                val newUsage = usage() + contentLength
                val capacity = Files.getFileStore(rootDirectory).usableSpace.toDouble()
                val percentage = newUsage / capacity
                return percentage < maxPercentage
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return true
        }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        init {
            if (maxPercentage > 1 || maxPercentage <= 0) {
                throw RuntimeException()
            }
        }
    }

}