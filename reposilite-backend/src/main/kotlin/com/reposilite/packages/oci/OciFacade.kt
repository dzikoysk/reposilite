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

package com.reposilite.packages.oci

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.packages.oci.api.ManifestResponse
import com.reposilite.packages.oci.api.SaveManifestRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.toLocation
import panda.std.Result
import panda.std.asSuccess

class OciFacade(
    private val journalist: Journalist,
    private val storageProvider: StorageProvider
) : Journalist, Facade {

    fun saveManifest(saveManifestRequest: SaveManifestRequest): Result<ManifestResponse, ErrorResponse> {
        storageProvider.putFile("oci/manifest.json".toLocation(), saveManifestRequest.toString().toByteArray().inputStream())
        return saveManifestRequest.let { ManifestResponse(it.schemaVersion, it.mediaType, it.config, it.layers) }.asSuccess()
    }

    override fun getLogger(): Logger =
        journalist.logger

}
