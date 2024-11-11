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

package com.reposilite.packages.oci.infrastructure

import com.reposilite.packages.oci.OciFacade
import com.reposilite.packages.oci.api.ManifestResponse
import com.reposilite.packages.oci.api.SaveManifestRequest
import com.reposilite.shared.badRequest
import com.reposilite.shared.extractFromHeader
import com.reposilite.shared.notFoundError
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.*
import io.javalin.http.bodyAsClass
import panda.std.Result.supplyThrowing

internal class OciEndpoints(
    private val ociFacade: OciFacade,
    private val basePath: String,
    private val compressionStrategy: String
) : ReposiliteRoutes() {

    fun saveManifest(namespace: String, reference: String) =
        ReposiliteRoute<ManifestResponse>("/api/oci/v2/$namespace/manifests/$reference", PUT) {
            accessed {
                val contentType = ctx.header("Content-Type")
                if (contentType != "application/vnd.docker.distribution.manifest.v2+json") {
                    response = notFoundError("Invalid content type")
                    return@accessed
                }

                response = supplyThrowing { ctx.bodyAsClass<SaveManifestRequest>() }
                    .mapErr { badRequest("Request does not contain valid body") }
                    .flatMap { saveManifestRequest ->
                        ociFacade.validateDigest(reference)
                            .flatMap { ociFacade.saveManifest(namespace, reference, saveManifestRequest) }
                            .flatMapErr { ociFacade.saveTaggedManifest(namespace, reference, saveManifestRequest) }
                    }
            }
        }

    override val routes = routes(saveManifest())

}
