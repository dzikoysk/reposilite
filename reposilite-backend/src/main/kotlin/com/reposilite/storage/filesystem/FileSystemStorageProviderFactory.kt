/*
 * Copyright (c) 2020-2026 dzikoysk
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
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.regex.Pattern
import kotlin.io.path.createDirectories

class FileSystemStorageProviderFactory : StorageProviderFactory<FileSystemStorageProvider, FileSystemStorageProviderSettings> {

    internal companion object {
        private val DISPLAY_SIZE_PATTERN = Pattern.compile("(\\d+)(([KkMmGg])[Bb])")
        private const val KB_FACTOR: Long = 1024
        private const val MB_FACTOR = 1024 * KB_FACTOR
        private const val GB_FACTOR = 1024 * MB_FACTOR
        private val DEFAULT_FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------")

        /**
         * @param rootDirectory root directory of storage space
         * @param quota quota to use as % or in bytes
         */
        fun of(
            journalist: Journalist,
            rootDirectory: Path,
            quota: String,
            filePermissions: Set<PosixFilePermission> = DEFAULT_FILE_PERMISSIONS,
        ): FileSystemStorageProvider =
            if (quota.endsWith("%")) {
                of(
                    journalist = journalist,
                    rootDirectory = rootDirectory,
                    maxPercentage = quota.substring(0, quota.length - 1).toInt() / 100.0,
                    filePermissions = filePermissions,
                )
            } else {
                of(
                    journalist = journalist,
                    rootDirectory = rootDirectory,
                    maxSize = displaySizeToBytesCount(quota),
                    filePermissions = filePermissions,
                )
            }

        /**
         * @param rootDirectory root directory of storage space
         * @param maxSize the largest amount of storage available for use, in bytes
         */
        fun of(
            journalist: Journalist,
            rootDirectory: Path,
            maxSize: Long,
            filePermissions: Set<PosixFilePermission> = DEFAULT_FILE_PERMISSIONS,
        ): FileSystemStorageProvider =
            FixedQuota(
                journalist = journalist,
                rootDirectory = rootDirectory,
                maxSize = maxSize,
                filePermissions = filePermissions,
            )

        /**
         * @param rootDirectory root directory of storage space
         * @param maxPercentage the maximum percentage of the disk available for use
         */
        fun of(
            journalist: Journalist,
            rootDirectory: Path,
            maxPercentage: Double,
            filePermissions: Set<PosixFilePermission> = DEFAULT_FILE_PERMISSIONS,
        ): FileSystemStorageProvider =
            PercentageQuota(
                journalist = journalist,
                rootDirectory = rootDirectory,
                maxPercentage = maxPercentage,
                filePermissions = filePermissions,
            )

        fun parseFilePermissions(value: String): Set<PosixFilePermission> {
            val trimmed = value.trim()
            val normalized = if (trimmed.length == 4 && trimmed.startsWith("0")) trimmed.substring(1) else trimmed
            require(normalized.matches(Regex("[0-7]{3}"))) {
                "File permissions must be an octal value with three digits (for example: 644)"
            }

            val mode = normalized.toInt(8)
            val permissions = mutableSetOf<PosixFilePermission>()
            if ((mode and 256) != 0) permissions += PosixFilePermission.OWNER_READ
            if ((mode and 128) != 0) permissions += PosixFilePermission.OWNER_WRITE
            if ((mode and 64) != 0) permissions += PosixFilePermission.OWNER_EXECUTE
            if ((mode and 32) != 0) permissions += PosixFilePermission.GROUP_READ
            if ((mode and 16) != 0) permissions += PosixFilePermission.GROUP_WRITE
            if ((mode and 8) != 0) permissions += PosixFilePermission.GROUP_EXECUTE
            if ((mode and 4) != 0) permissions += PosixFilePermission.OTHERS_READ
            if ((mode and 2) != 0) permissions += PosixFilePermission.OTHERS_WRITE
            if ((mode and 1) != 0) permissions += PosixFilePermission.OTHERS_EXECUTE
            return permissions
        }

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
        val filePermissions = parseFilePermissions(settings.filePermissions)
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
            filePermissions = filePermissions,
        )
    }

    override val settingsType: Class<FileSystemStorageProviderSettings> =
        FileSystemStorageProviderSettings::class.java

    override val type: String =
        "fs"

}
