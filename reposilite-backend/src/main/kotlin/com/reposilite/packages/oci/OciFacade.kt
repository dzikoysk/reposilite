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
import com.reposilite.packages.oci.api.BlobResponse
import com.reposilite.packages.oci.api.ManifestResponse
import com.reposilite.packages.oci.api.SaveManifestRequest
import com.reposilite.packages.oci.api.UploadState
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.notFound
import com.reposilite.shared.notFoundError
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.toLocation
import panda.std.Result
import panda.std.asSuccess
import java.security.MessageDigest
import java.util.*

class OciFacade(
    private val journalist: Journalist,
    private val storageProvider: StorageProvider
) : Journalist, Facade {

    private val sessions = mutableMapOf<String, UploadState>()
    private val sha256Hash = MessageDigest.getInstance("SHA-256")

    fun saveManifest(namespace: String, digest: String, saveManifestRequest: SaveManifestRequest): Result<ManifestResponse, ErrorResponse> {
        storageProvider.putFile("manifests/${namespace}/${digest}".toLocation(), saveManifestRequest.toString().toByteArray().inputStream())
        return saveManifestRequest.let { ManifestResponse(it.schemaVersion, it.mediaType, it.config, it.layers) }.asSuccess()
    }

    fun saveTaggedManifest(namespace: String, tag: String, saveManifestRequest: SaveManifestRequest): Result<ManifestResponse, ErrorResponse> {
        val digest = sha256Hash.digest(saveManifestRequest.toString().toByteArray()).joinToString("") { "%02x".format(it) }

        storageProvider.putFile("manifests/${namespace}/${tag}/manifest".toLocation(), saveManifestRequest.toString().toByteArray().inputStream())
        storageProvider.putFile("manifests/${namespace}/${tag}/manifest.sha256".toLocation(), digest.toByteArray().inputStream())
        return saveManifestRequest.let { ManifestResponse(it.schemaVersion, it.mediaType, it.config, it.layers) }.asSuccess()
    }

    fun retrieveBlobUploadSessionId(namespace: String): Result<String, ErrorResponse> {
        val sessionId = UUID.randomUUID().toString()

        sessions[sessionId] = UploadState(
            sessionId = sessionId,
            name = namespace,
            uploadedData = ByteArray(0),
            bytesReceived = 0,
            createdAt = System.currentTimeMillis().toString()
        )

        return sessionId.asSuccess()
    }

    fun uploadBlobStreamPart(sessionId: String, part: ByteArray): Result<UploadState, ErrorResponse> {
        val session = sessions[sessionId] ?: return notFoundError("Session not found")

        session.uploadedData += part
        session.bytesReceived += part.size

        return session.asSuccess()
    }

    fun findBlobByDigest(namespace: String, digest: String): Result<BlobResponse, ErrorResponse> =
        storageProvider.getFile("blobs/${namespace}/${digest}".toLocation())
            .map {
                BlobResponse(
                    digest = digest,
                    length = it.available(),
                    content = it
                )
            }
            .mapErr { notFound("Could not find blob with specified digest") }

    fun findManifestChecksumByDigest(namespace: String, digest: String): Result<String, ErrorResponse> {
        val location = "manifests/${namespace}/${digest}".toLocation()
        return storageProvider.getFile(location)
            .map { it.readAllBytes().joinToString("") { "%02x".format(it) } }
    }

    fun findManifestChecksumByTag(namespace: String, tag: String): Result<String, ErrorResponse> {
        val location = "manifests/${namespace}/${tag}/manifest.sha256".toLocation()

        return storageProvider.getFile(location)
            .map { it.readAllBytes().joinToString("") { "%02x".format(it) } }
    }

    fun findManifestTagByDigest(namespace: String, digest: String): Result<String, ErrorResponse> {
        val tagsDirectory = "manifests/${namespace}".toLocation()

        // todo replace with exposed (digest to tag mapping)
        return storageProvider.getFiles(tagsDirectory)
            .flatMap { files ->
                files
                    .map { storageProvider.getFile(it.resolve("manifest.sha256")) }
                    .map { it.map { it.readAllBytes().joinToString("") { "%02x".format(it) } } }
                    .first()
            }
    }

    fun validateDigest(digest: String): Result<String, ErrorResponse> {
        if (!digest.startsWith("sha256:")) {
            return badRequestError("Invalid digest format")
        }

        return digest.asSuccess()
    }

    override fun getLogger(): Logger =
        journalist.logger

}
