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
package com.reposilite.shared.fs

import com.reposilite.shared.fs.FileType.DIRECTORY
import com.reposilite.shared.fs.FileType.FILE
import com.reposilite.shared.fs.VersionComparator.Companion.asVersion
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import panda.std.Result
import panda.std.asSuccess
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

sealed class FileDetails(
    val type: FileType,
    val name: String,
) : Comparable<FileDetails> {

    override fun compareTo(other: FileDetails): Int =
        type.compareTo(other.type).takeIf { it != 0 } ?: name.compareTo(other.name)

}

const val UNKNOWN_LENGTH = -1L

class DocumentInfo(
    name: String,
    val contentType: ContentType,
    val contentLength: Long,
) : FileDetails(FILE, name)

sealed class AbstractDirectoryInfo(
    name: String,
) : FileDetails(DIRECTORY, name)

class SimpleDirectoryInfo(
    name: String,
) : AbstractDirectoryInfo(name)

class DirectoryInfo(
    name: String,
    val files: List<FileDetails>
) : AbstractDirectoryInfo(name) {

    fun filter(predicate: (FileDetails) -> Boolean): DirectoryInfo =
        DirectoryInfo(name, files.filter { predicate(it) })

}

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
            ContentType.getContentTypeByExtension(file.getExtension()) ?: APPLICATION_OCTET_STREAM,
            Files.size(file)
        ).asSuccess()
    }

private fun toDirectoryInfo(directory: Path): Result<DirectoryInfo, ErrorResponse> =
    catchIOException {
        DirectoryInfo(
            directory.getSimpleName(),
            Files.list(directory).asSequence()
                .map { toSimpleFileDetails(it).orElseThrow { error -> IOException(error.message) } }
                .sortedWith(FilesComparator({ asVersion(it.name) }, { it.type == DIRECTORY }))
                .toList()
        ).asSuccess()
    }

private fun toSimpleFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
    when (file.type()) {
        FILE -> toDocumentInfo(file)
        DIRECTORY -> SimpleDirectoryInfo(file.getSimpleName()).asSuccess()
    }