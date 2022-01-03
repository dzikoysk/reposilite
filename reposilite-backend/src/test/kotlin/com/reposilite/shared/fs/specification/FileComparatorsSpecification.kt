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

package com.reposilite.shared.fs.specification

import com.reposilite.storage.FilesComparator
import com.reposilite.storage.VersionComparator

internal abstract class FileComparatorsSpecification {

    val filesComparator = FilesComparator<FileInfo>(
        { VersionComparator.asVersion(it.name) },
        { it.isDirectory }
    )

    data class FileInfo(
        val name: String,
        val isDirectory: Boolean
    )

    fun file(name: String): FileInfo =
        FileInfo(name, false)

    fun directory(name: String): FileInfo =
        FileInfo(name, true)

}