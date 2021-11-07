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

import com.reposilite.shared.fs.FilesUtils
import java.nio.file.Path

internal object FileSystemStorageProviderFactory {

    fun of(rootDirectory: Path, quota: String): FileSystemStorageProvider =
        if (quota.endsWith("%")) {
            of(rootDirectory, quota.substring(0, quota.length - 1).toInt() / 100.0)
        } else {
            of(rootDirectory, FilesUtils.displaySizeToBytesCount(quota))
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

}