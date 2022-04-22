/*
 * Copyright (c) 2022 dzikoysk
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
package com.reposilite.storage.api

import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.FileType.FILE
import io.javalin.http.ContentType

const val UNKNOWN_LENGTH = -1L

enum class FileType {
    FILE,
    DIRECTORY
}

sealed class FileDetails(
    val type: FileType,
    val name: String,
) : Comparable<FileDetails> {

    override fun compareTo(other: FileDetails): Int =
        type.compareTo(other.type)
            .takeIf { it != 0 }
            ?: name.compareTo(other.name)

}

class DocumentInfo(
    name: String,
    val contentType: ContentType,
    val contentLength: Long = UNKNOWN_LENGTH,
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