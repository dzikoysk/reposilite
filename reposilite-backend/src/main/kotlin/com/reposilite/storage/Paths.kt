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

import com.reposilite.storage.api.FileType
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.FileType.FILE
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFound
import io.javalin.http.HttpCode.NO_CONTENT
import panda.std.Result
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.type(): FileType =
    if (this.isDirectory()) DIRECTORY else FILE

fun Path.inputStream(): Result<InputStream, ErrorResponse> =
    Result.`when`(Files.exists(this), this, notFound(""))
        .filter({ it.isDirectory().not() }, { ErrorResponse(NO_CONTENT, "Requested file is a directory") })
        .map { Files.newInputStream(it) }

internal fun Path.getExtension(): String =
    getSimpleName().getExtension()

fun Path.getSimpleName(): String =
    fileName.toString()

fun String.getExtension(): String =
    lastIndexOf(".")
        .takeIf { it != -1 }
        ?.let { substring(it + 1) }
        ?: ""