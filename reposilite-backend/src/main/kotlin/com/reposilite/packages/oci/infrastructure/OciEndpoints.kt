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
import com.reposilite.shared.badRequestError
import com.reposilite.web.api.ReposiliteRoute
import io.javalin.community.routing.Route.*
import io.javalin.http.HandlerType
import io.javalin.http.bodyAsClass
import panda.std.Result.supplyThrowing

internal class OciEndpoints(
    private val ociFacade: OciFacade,
) {

    fun saveManifest(namespace: String) =
        ReposiliteRoute<ManifestResponse>("/api/oci/v2/$namespace/manifests/{reference}", PUT) {
            accessed {
                val contentType = ctx.header("Content-Type")
                if (contentType != "application/vnd.docker.distribution.manifest.v2+json") {
                    response = badRequestError("Invalid content type")
                    return@accessed
                }

                val reference = parameter("reference") ?: return@accessed

                response = supplyThrowing { ctx.bodyAsClass<SaveManifestRequest>() }
                    .mapErr { badRequest("Request does not contain valid body") }
                    .flatMap { saveManifestRequest ->
                        ociFacade.validateDigest(reference)
                            .fold(
                                { ociFacade.saveManifest(namespace, reference, saveManifestRequest) },
                                { ociFacade.saveTaggedManifest(namespace, reference, saveManifestRequest) }
                            )
                    }
            }
        }

    fun retrieveBlobUploadSessionId(namespace: String) =
        ReposiliteRoute<Unit>("/api/oci/v2/$namespace/blobs/uploads", POST) {
            accessed {
                val digest = queryParameter("digest")
                if (digest == null) {
                    response = ociFacade.retrieveBlobUploadSessionId(namespace)
                        .map {
                            ctx.status(202)
                            ctx.header("Location", "/api/oci/v2/$namespace/blobs/uploads/$it")
                        }

                    return@accessed
                }
            }
        }

    fun uploadBlobStreamPart(namespace: String) =
        ReposiliteRoute<Unit>("/api/oci/v2/$namespace/blobs/uploads/{sessionId}", PATCH) {
            accessed {
                val contentType = ctx.header("Content-Type")
                if (contentType != "application/octet-stream") {
                    response = badRequestError("Invalid content type")
                    return@accessed
                }

                val sessionId = parameter("sessionId") ?: return@accessed

                response = supplyThrowing { ctx.bodyAsBytes() }
                    .mapErr { badRequest("Body does not contain any bytes") }
                    .flatMap { ociFacade.uploadBlobStreamPart(sessionId, it) }
                    .map {
                        ctx.status(202)
                        ctx.header("Location", "/api/oci/v2/$namespace/blobs/uploads/$sessionId")
                        ctx.header("Range", "0-${it.bytesReceived - 1}")
                    }
            }
        }

    fun finalizeBlobUpload(namespace: String) =
        ReposiliteRoute<Unit>("/api/oci/v2/$namespace/blobs/{sessionId}", PUT) {
            accessed {
                val sessionId = parameter("sessionId") ?: return@accessed
                val digest = queryParameter("digest")
                if (digest == null) {
                    response = badRequestError("No digest provided")
                    return@accessed
                }

                response = supplyThrowing { ctx.bodyAsBytes() }
                    .fold(
                        { ociFacade.finalizeBlobUpload(namespace, digest, sessionId, it) },
                        { ociFacade.finalizeBlobUpload(namespace, digest, sessionId, null) },
                    )
                    .map {
                        ctx.status(201)
                        ctx.header("Location", "/api/oci/v2/$namespace/blobs/${it.digest}")
                    }
            }
        }

    fun findBlobByDigest(namespace: String) =
        ReposiliteRoute<ByteArray>("/api/oci/v2/$namespace/blobs/{digest}", GET, HEAD) {
            accessed {
                val digest = parameter("digest") ?: return@accessed

                response = ociFacade.findBlobByDigest(namespace, digest)
                    .peek {
                        ctx.header("Content-Length", it.length.toString())
                        ctx.header("Docker-Content-Digest", it.digest)
                    }
                    .takeIf { ctx.method() == HandlerType.GET }
                    ?.map { it.content.readNBytes(it.length) }
            }
        }

    fun findManifestChecksumByReference(namespace: String) =
        ReposiliteRoute<Unit>("/api/oci/v2/$namespace/manifests/{reference}", HEAD) {
            accessed {
                val reference = parameter("reference") ?: return@accessed

                response = ociFacade.validateDigest(reference)
                    .fold(
                        { ociFacade.findManifestChecksumByDigest(namespace, reference) },
                        { ociFacade.findManifestChecksumByTag(namespace, reference) }
                    )
                    .map {
                        ctx.status(200)
                        ctx.header("Docker-Content-Digest", it)
                    }
            }
        }

}
