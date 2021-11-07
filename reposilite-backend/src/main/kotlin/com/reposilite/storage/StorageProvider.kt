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

package com.reposilite.storage

import com.reposilite.shared.fs.FileDetails
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.attribute.FileTime

interface StorageProvider {

    companion object {
        const val DEFAULT_STORAGE_PROVIDER_BUFFER_SIZE = 1024 * 32 // 64kb
    }

    fun putFile(path: Path, inputStream: InputStream): Result<Unit, ErrorResponse>

    fun getFile(path: Path): Result<InputStream, ErrorResponse>

    fun getFileDetails(path: Path): Result<out FileDetails, ErrorResponse>

    fun removeFile(path: Path): Result<Unit, ErrorResponse>

    fun getFiles(path: Path): Result<List<Path>, ErrorResponse>

    fun getLastModifiedTime(path: Path): Result<FileTime, ErrorResponse>

    fun getFileSize(path: Path): Result<Long, ErrorResponse>

    fun exists(file: Path): Boolean

    fun usage(): Result<Long, ErrorResponse>

    fun canHold(contentLength: Long): Result<Long, ErrorResponse>

    fun shutdown() {}

}