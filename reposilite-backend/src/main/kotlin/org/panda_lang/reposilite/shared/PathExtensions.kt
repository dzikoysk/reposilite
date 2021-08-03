package org.panda_lang.reposilite.shared

import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.NOT_FOUND
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.failure.api.errorResponse
import org.panda_lang.reposilite.shared.FileType.DIRECTORY
import org.panda_lang.reposilite.shared.FileType.FILE
import panda.std.Result
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.isDirectory
import kotlin.streams.toList
import panda.std.Result.`when` as once

enum class FileType {
    FILE,
    DIRECTORY
}

fun Path.exists(): Result<Path, ErrorResponse> =
    once(Files.exists(this), this, ErrorResponse(NOT_FOUND, "File not found: $this"))

fun Path.type(): FileType =
    if (this.isDirectory()) DIRECTORY else FILE

fun Path.delete(): Result<*, ErrorResponse> =
    catchIOException {
        exists().map { Files.delete(this) }
    }

fun Path.getLastModifiedTime(): Result<FileTime, ErrorResponse> =
    catchIOException {
        exists().map { Files.getLastModifiedTime(this) }
    }

fun Path.listFiles(): Result<List<Path>, ErrorResponse> =
    catchIOException {
        exists().map {
            Files.walk(this, 1).filter { it != this }.toList()
        }
    }

fun Path.inputStream(): Result<InputStream, ErrorResponse> =
    catchIOException {
        exists()
            .filter({ it.isDirectory().not() }, { ErrorResponse(HttpCode.NO_CONTENT, "Requested file is a directory") })
            .map { Files.newInputStream(it) }
    }

fun Path.size(): Result<Long, ErrorResponse> =
    catchIOException {
        exists().map {
            when (type()) {
                FILE -> Files.size(this)
                DIRECTORY -> Files.walk(this).mapToLong { Files.size(it) }.sum()
            }
        }
    }

fun <VALUE> catchIOException(consumer: () -> Result<VALUE, ErrorResponse>): Result<VALUE, ErrorResponse> =
    try {
        consumer()
    } catch (ioException: IOException) {
        errorResponse(HttpCode.INTERNAL_SERVER_ERROR, ioException.localizedMessage)
    }