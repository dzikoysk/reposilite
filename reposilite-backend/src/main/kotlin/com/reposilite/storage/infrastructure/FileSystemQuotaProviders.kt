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

package com.reposilite.storage.infrastructure

import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.INSUFFICIENT_STORAGE
import panda.std.Result
import java.nio.file.Files
import java.nio.file.Path

/**
 * @param rootDirectory root directory of storage space
 * @param maxSize the largest amount of storage available for use, in bytes
 */
internal class FixedQuota(rootDirectory: Path, private val maxSize: Long) : FileSystemStorageProvider(rootDirectory) {

    init {
        if (maxSize <= 0) {
            throw IllegalArgumentException("Max size parameter has to be a value greater than 0")
        }
    }

    override fun canHold(contentLength: Long): Result<Long, ErrorResponse> =
        usage()
            .map { usage -> maxSize - usage }
            .filter({ available -> contentLength <= available }, { ErrorResponse(INSUFFICIENT_STORAGE, "Repository cannot hold the given file (${maxSize - it} + $contentLength > $maxSize)") })

}

/**
 * @param rootDirectory root directory of storage space
 * @param maxPercentage the maximum percentage of the disk available for use
 */
internal class PercentageQuota(
    private val rootDirectory: Path,
    private val maxPercentage: Double
) : FileSystemStorageProvider(rootDirectory) {

    init {
        if (maxPercentage > 1 || maxPercentage <= 0) {
            throw IllegalArgumentException("Percentage parameter has to be a value between 0.0 and 1.0")
        }
    }

    override fun canHold(contentLength: Long): Result<Long, ErrorResponse> =
        usage()
            .map { usage ->
                val capacity = Files.getFileStore(rootDirectory).usableSpace
                val max = capacity * maxPercentage
                max.toLong() - usage
            }
            .filter({ available -> contentLength <= available }, { ErrorResponse(INSUFFICIENT_STORAGE, "Repository cannot hold the given file ($contentLength too much for $maxPercentage%)") })

}
