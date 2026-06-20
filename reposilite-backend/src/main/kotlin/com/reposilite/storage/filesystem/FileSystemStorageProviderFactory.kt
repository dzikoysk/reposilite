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

package com.reposilite.storage.filesystem

import com.reposilite.journalist.Journalist
import com.reposilite.status.FailureFacade
import com.reposilite.storage.StorageProviderFactory
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.createDirectories

class FileSystemStorageProviderFactory : StorageProviderFactory<FileSystemStorageProvider, FileSystemStorageProviderSettings> {

    internal companion object {
        private val DISPLAY_SIZE_PATTERN = Pattern.compile("(\\d+)(([KkMmGg])[Bb])")
        private const val KB_FACTOR: Long = 1024
        private const val MB_FACTOR = 1024 * KB_FACTOR
        private const val GB_FACTOR = 1024 * MB_FACTOR

        fun of(journalist: Journalist, rootDirectory: Path, quota: String?): FileSystemStorageProvider =
            if (quota == null) {
                NoQuota(journalist, rootDirectory)
            } else if (quota.endsWith("%")) {
                val percentage = quota.substring(0, quota.length - 1).toInt() / 100.0
                val usageTracker = UsageTracker(rootDirectory)
                PercentageQuota(journalist, rootDirectory, usageTracker, percentage)
            } else {
                val maxSize = displaySizeToBytesCount(quota)
                val usageTracker = UsageTracker(rootDirectory)
                FixedQuota(journalist, rootDirectory, usageTracker, maxSize)
            }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        fun of(journalist: Journalist, rootDirectory: Path, maxSize: Long): FileSystemStorageProvider =
            FixedQuota(
                journalist = journalist,
                rootDirectory = rootDirectory,
                usageTracker = UsageTracker(rootDirectory),
                maxSize = maxSize,
            )

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        fun of(journalist: Journalist, rootDirectory: Path, maxPercentage: Double): FileSystemStorageProvider =
            PercentageQuota(
                journalist = journalist,
                rootDirectory = rootDirectory,
                usageTracker = UsageTracker(rootDirectory),
                maxPercentage = maxPercentage,
            )

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

    override fun create(
        journalist: Journalist,
        failureFacade: FailureFacade,
        workingDirectory: Path,
        repositoryName: String,
        settings: FileSystemStorageProviderSettings,
    ): FileSystemStorageProvider {
        val repositoryDirectory =
            if (settings.mount.isEmpty())
                workingDirectory.resolve(repositoryName)
            else
                workingDirectory.resolve(settings.mount)

        repositoryDirectory.createDirectories()

        return of(
            journalist = journalist,
            rootDirectory = repositoryDirectory,
            quota = settings.quota,
        )
    }

    override val settingsType: Class<FileSystemStorageProviderSettings> =
        FileSystemStorageProviderSettings::class.java

    override val type: String =
        "fs"

}
