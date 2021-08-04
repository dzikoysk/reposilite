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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.reposilite.failure.api.ErrorResponse
import com.reposilite.shared.FileType
import com.reposilite.shared.FileType.DIRECTORY
import com.reposilite.shared.FileType.FILE
import com.reposilite.shared.FilesUtils
import com.reposilite.shared.FilesUtils.getSimpleName
import com.reposilite.shared.catchIOException
import com.reposilite.shared.exists
import com.reposilite.shared.type
import com.reposilite.web.api.MimeTypes.OCTET_STREAM
import com.reposilite.web.asResult
import panda.std.Result
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

sealed class FileDetails(
    val type: FileType,
    val name: String
) : Comparable<FileDetails> {

    override fun compareTo(other: FileDetails): Int =
        type.compareTo(other.type).takeIf { it != 0 } ?: name.compareTo(other.name)

    @JsonIgnore
    fun isReadable(): Boolean =
        FilesUtils.isReadable(name)

}

class DocumentInfo(
    name: String,
    val contentType: String,
    val contentLength: Long,
    @JsonIgnore
    val content: () -> InputStream
) : FileDetails(FILE, name)

sealed class AbstractSimpleDirectoryInfo(
    name: String,
) : FileDetails(DIRECTORY, name)

class SimpleDirectoryInfo(
    name: String,
) : AbstractSimpleDirectoryInfo(name)

class DirectoryInfo(
    name: String,
    val files: List<FileDetails>
) : AbstractSimpleDirectoryInfo(name)

fun toFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
    file.exists()
        .flatMap {
            when (it.type()) {
                FILE -> toDocumentInfo(it)
                DIRECTORY -> toDirectoryInfo(it)
            }
        }

fun toDocumentInfo(file: Path): Result<DocumentInfo, ErrorResponse> =
    catchIOException {
        DocumentInfo(
            file.getSimpleName(),
            FilesUtils.getMimeType(file.getSimpleName(), OCTET_STREAM),
            Files.size(file),
            { Files.newInputStream(file) }
        ).asResult()
    }

private fun toDirectoryInfo(directory: Path): Result<DirectoryInfo, ErrorResponse> =
    catchIOException {
        DirectoryInfo(
            directory.getSimpleName(),
            Files.list(directory)
                .map { toSimpleFileDetails(it).orElseThrow { error -> IOException(error.message) } }
                .toList()
        ).asResult()
    }

private fun toSimpleFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
    when (file.type()) {
        FILE -> toDocumentInfo(file)
        DIRECTORY -> SimpleDirectoryInfo(file.getSimpleName()).asResult()
    }