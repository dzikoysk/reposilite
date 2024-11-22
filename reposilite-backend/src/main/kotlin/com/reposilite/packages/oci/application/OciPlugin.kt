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

package com.reposilite.packages.oci.application

import com.reposilite.packages.oci.OciFacade
import com.reposilite.packages.oci.OciRepositoryProvider
import com.reposilite.packages.oci.infrastructure.OciEndpoints
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.token.infrastructure.AccessTokenApiEndpoints
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(
    name = "oci",
    dependencies = ["failure", "local-configuration", "shared-configuration", "statistics", "authentication", "access-token", "storage"]
)
internal class OciPlugin : ReposilitePlugin() {

    override fun initialize(): OciFacade {
        val ociRepositoryProvider = OciRepositoryProvider()

        val ociFacade = OciFacade(
            journalist = this,
            storageProvider = facade(),
            ociRepositoryProvider = ociRepositoryProvider
        )

        // register endpoints
        event { event: RoutingSetupEvent ->
            ociFacade.getRepositories().forEach {
                when (it.type) {
                    "oci" -> {
                        val ociEndpoints = OciEndpoints(ociFacade)

                        event.register(ociEndpoints.saveManifest(it.name))
                        event.register(ociEndpoints.retrieveBlobUploadSessionId(it.name))
                        event.register(ociEndpoints.findManifestChecksumByReference(it.name))
                        event.register(ociEndpoints.findBlobByDigest(it.name))
                        event.register(ociEndpoints.uploadBlobStreamPart(it.name))
                        event.register(ociEndpoints.finalizeBlobUpload(it.name))
                    }
                }
            }
        }

        return ociFacade
    }

}
