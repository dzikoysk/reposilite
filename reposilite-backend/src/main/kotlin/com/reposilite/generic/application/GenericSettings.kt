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

package com.reposilite.generic.application

import com.reposilite.configuration.shared.api.Doc
import com.reposilite.configuration.shared.api.Min
import com.reposilite.configuration.shared.api.SharedSettings
import com.reposilite.repository.api.RepositoryAccessMode
import com.reposilite.repository.api.RepositoryAccessMode.PUBLIC
import com.reposilite.storage.StorageProviderSettings
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import com.reposilite.storage.s3.S3StorageProviderSettings
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OneOf

@JsonSchema(requireNonNulls = false)
@Doc(title = "Generic", description = "Generic file repository settings")
data class GenericSettings(
    @get:Doc(title = "Repositories", description = "List of generic file repositories.")
    val repositories: List<GenericRepositorySettings> = emptyList(),
) : SharedSettings

@Doc(title = "Generic Repository", description = "Settings for a generic file repository.")
data class GenericRepositorySettings(
    @Min(1)
    @get:Doc(title = "Id", description = "The id of this repository.")
    val id: String = "",
    @get:Doc(title = "Visibility", description = "The visibility of this repository.")
    val visibility: RepositoryAccessMode = PUBLIC,
    @get:Doc(title = "Redeployment", description = "Whether an existing file can be overwritten.")
    val redeployment: Boolean = false,
    @get:Doc(title = "Storage provider", description = "The storage used by this repository.")
    @get:OneOf(FileSystemStorageProviderSettings::class, S3StorageProviderSettings::class)
    val storageProvider: StorageProviderSettings = FileSystemStorageProviderSettings(),
) : SharedSettings
