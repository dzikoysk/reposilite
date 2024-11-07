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

package com.reposilite.packages.oci.api

data class SaveManifestRequest(
    val schemaVersion: Int,
    val mediaType: String,
    val config: ManifestConfig,
    val layers: List<ManifestLayer>,
)

data class ManifestResponse(
    val schemaVersion: Int,
    val mediaType: String,
    val config: ManifestConfig,
    val layers: List<ManifestLayer>,
)

data class ManifestConfig(
    val mediaType: String,
    val size: Int,
    val digest: String,
)

data class ManifestLayer(
    val mediaType: String,
    val size: Int,
    val digest: String,
)
