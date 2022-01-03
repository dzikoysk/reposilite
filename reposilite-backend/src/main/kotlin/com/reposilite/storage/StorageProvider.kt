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

import com.reposilite.storage.api.FileDetails
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.io.InputStream
import java.nio.file.attribute.FileTime

interface StorageProvider {

    fun putFile(location: Location, inputStream: InputStream): Result<Unit, ErrorResponse>

    fun getFile(location: Location): Result<InputStream, ErrorResponse>

    fun getFileDetails(location: Location): Result<out FileDetails, ErrorResponse>

    fun removeFile(location: Location): Result<Unit, ErrorResponse>

    fun getFiles(location: Location): Result<List<Location>, ErrorResponse>

    fun getLastModifiedTime(location: Location): Result<FileTime, ErrorResponse>

    fun getFileSize(location: Location): Result<Long, ErrorResponse>

    fun exists(location: Location): Boolean

    fun usage(): Result<Long, ErrorResponse>

    fun canHold(contentLength: Long): Result<Long, ErrorResponse>

    fun shutdown() {}

}