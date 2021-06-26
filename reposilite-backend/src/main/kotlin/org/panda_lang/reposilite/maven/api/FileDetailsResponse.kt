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
package org.panda_lang.reposilite.maven.api

import org.panda_lang.reposilite.shared.FilesUtils
import java.io.Serializable
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class FileDetailsResponse(
    val type: String,
    val name: String,
    val date: String,
    val contentType: String,
    val contentLength: Long
) : Serializable, Comparable<FileDetailsResponse> {

    companion object {
        const val FILE = "file"
        const val DIRECTORY = "directory"

        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    override fun compareTo(other: FileDetailsResponse): Int {
        var result = type.compareTo(other.type)

        if (result == 0) {
            result = name.compareTo(other.name)
        }

        return result
    }

    fun isReadable(): Boolean =
        FilesUtils.isReadable(name)

    fun isDirectory(): Boolean =
        DIRECTORY == type

}