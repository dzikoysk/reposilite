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

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.storage.StorageProviderSettings
import io.javalin.openapi.Custom

@Doc(title = "File system Storage Provider", description = "Local file system (disk) storage provider settings")
data class FileSystemStorageProviderSettings(
    @get:Custom(name = "const", value = "fs")
    override val type: String = "fs",
    @get:Doc(title = "Quota", description = "Control the maximum amount of data stored in this repository. Supported formats: 90%, 500MB, 10GB (optional, by default: unlimited)")
    val quota: String = "100%",
    @get:Doc(title = "Mount", description = "Use custom directory to locate the repository data (optional, by default it's './repositories/{name}')")
    val mount: String = ""
) : StorageProviderSettings
