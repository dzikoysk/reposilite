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

import java.nio.file.Path
import java.util.regex.Pattern

internal object FileSystemStorageProviderFactory {

    private val DISPLAY_SIZE_PATTERN = Pattern.compile("([0-9]+)(([KkMmGg])[Bb])")
    private const val KB_FACTOR: Long = 1024
    private const val MB_FACTOR = 1024 * KB_FACTOR
    private const val GB_FACTOR = 1024 * MB_FACTOR

    /**
     * @param rootDirectory root directory of storage space
     * @param quota quota to use as % or in bytes
     */
    fun of(rootDirectory: Path, quota: String): FileSystemStorageProvider =
        if (quota.endsWith("%")) {
            of(rootDirectory, quota.substring(0, quota.length - 1).toInt() / 100.0)
        } else {
            of(rootDirectory, displaySizeToBytesCount(quota))
        }

    /**
     * @param rootDirectory root directory of storage space
     * @param maxSize the largest amount of storage available for use, in bytes
     */
    fun of(rootDirectory: Path, maxSize: Long): FileSystemStorageProvider =
        FixedQuota(rootDirectory, maxSize)

    /**
     * @param rootDirectory root directory of storage space
     * @param maxPercentage the maximum percentage of the disk available for use
     */
    fun of(rootDirectory: Path, maxPercentage: Double): FileSystemStorageProvider =
        PercentageQuota(rootDirectory, maxPercentage)

    private fun displaySizeToBytesCount(displaySize: String): Long {
        val match = DISPLAY_SIZE_PATTERN.matcher(displaySize)

        if (!match.matches() || match.groupCount() != 3) {
            return displaySize.toLong()
        }

        val value = match.group(1).toLong()

        return when (match.group(2).uppercase()) {
            "GB" -> value * GB_FACTOR
            "MB" -> value * MB_FACTOR
            "KB" -> value * KB_FACTOR
            else -> throw NumberFormatException("Wrong format")
        }
    }

}