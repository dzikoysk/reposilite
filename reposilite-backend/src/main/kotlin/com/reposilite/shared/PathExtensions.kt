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

package com.reposilite.shared

import com.reposilite.shared.FileType.DIRECTORY
import com.reposilite.shared.FileType.FILE
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import panda.std.Result.error
import panda.std.Result.ok
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.stream.Collectors
import kotlin.io.path.isDirectory
import panda.std.Result.`when` as once

enum class FileType {
    FILE,
    DIRECTORY
}

/* internal */ fun String.toPath(): Path =
    Paths.get(this)

internal fun Path.exists(): Result<Path, ErrorResponse> =
    once(Files.exists(this), this, ErrorResponse(NOT_FOUND, "File not found: $this"))

internal fun Path.type(): FileType =
    if (this.isDirectory()) DIRECTORY else FILE

internal fun Path.delete(): Result<Unit, ErrorResponse> =
    catchIOException {
        exists().map { Files.delete(this) }
    }

internal fun Path.getLastModifiedTime(): Result<FileTime, ErrorResponse> =
    catchIOException {
        exists().map { Files.getLastModifiedTime(this) }
    }

internal fun Path.listFiles(): Result<List<Path>, ErrorResponse> =
    catchIOException {
        exists().map {
            Files.walk(this, 1)
                .filter { it != this }
                .collect(Collectors.toList())
        }
    }

internal fun Path.inputStream(): Result<InputStream, ErrorResponse> =
    catchIOException {
        exists()
            .filter({ it.isDirectory().not() }, { ErrorResponse(HttpCode.NO_CONTENT, "Requested file is a directory") })
            .map { Files.newInputStream(it) }
    }

internal fun Path.decodeToString(): Result<String, ErrorResponse> =
    inputStream()
        .map { it.use { input -> input.readBytes().decodeToString() } }

internal fun Path.size(): Result<Long, ErrorResponse> =
    catchIOException {
        exists().map {
            when (type()) {
                FILE -> Files.size(this)
                DIRECTORY -> Files.walk(this)
                    .mapToLong { Files.size(it) }
                    .sum()
            }
        }
    }

internal fun Path.append(path: String): Result<Path, IOException> =
    path.toNormalizedPath()
        .map { this.safeResolve(it).normalize() }

internal fun Path.safeResolve(file: Path): Path =
    safeResolve(file.toString())

internal fun Path.safeResolve(file: String): Path =
    resolve(
        if (file.startsWith(File.separator))
            file.substring(File.separator.length)
        else
            file
    )

internal fun Path.getExtension(): String =
    getSimpleName().getExtension()

/* internal */ fun Path.getSimpleName(): String =
    this.fileName.toString()

internal fun String.getSimpleNameFromUri(): String =
    this.substring(this.lastIndexOf('/') + 1)

internal fun String.toNormalizedPath(): Result<Path, IOException> =
    normalizedAsUri().map { it.toPath().normalize() }

/**
 * Process uri applying following changes:
 *
 *
 *  * Remove root slash
 *  * Remove illegal path modifiers like .. and ~
 *
 *
 * @return the normalized uri
 */
internal fun String.normalizedAsUri(): Result<String, IOException> {
    var normalizedUri = this

    if (normalizedUri.contains("..") || normalizedUri.contains(":") || normalizedUri.contains("\\")) {
        return error(IOException("Illegal path operator in URI"))
    }

    while (normalizedUri.contains("//")) {
        normalizedUri = normalizedUri.replace("//", "/")
    }

    if (normalizedUri.startsWith("/")) {
        normalizedUri = normalizedUri.substring(1)
    }

    return ok(normalizedUri)
}

internal fun <VALUE> catchIOException(consumer: () -> Result<VALUE, ErrorResponse>): Result<VALUE, ErrorResponse> =
    try {
        consumer()
    } catch (ioException: IOException) {
        errorResponse(HttpCode.INTERNAL_SERVER_ERROR, ioException.message ?: "<no message>")
    }