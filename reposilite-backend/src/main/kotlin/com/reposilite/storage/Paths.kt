/*
 * Copyright (c) 2023 dzikoysk
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

import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFound
import com.reposilite.shared.toErrorResponse
import com.reposilite.storage.api.FileType
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.FileType.FILE
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NO_CONTENT
import panda.std.Result
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.inputStream as newInputStream

fun Path.type(): FileType =
    if (this.isDirectory()) DIRECTORY else FILE

fun Path.inputStream(): Result<InputStream, ErrorResponse> =
    Result.`when`(this.exists(), this, notFound("File not found"))
        .filter({ !it.isDirectory() }, { NO_CONTENT.toErrorResponse("Requested file is a directory") })
        .flatMap {
            Result.supplyThrowing { it.newInputStream() }
                .onError { it.printStackTrace() }
                .mapErr { INTERNAL_SERVER_ERROR.toErrorResponse("Cannot read file") }
        }

fun String.getExtension(): String = substringAfterLast('.', "")
