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

package com.reposilite.maven

import com.reposilite.maven.api.Checksum
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.api.Location
import panda.std.Result
import panda.std.asSuccess
import java.io.InputStream

/**
 * Records the fingerprint of mirrored metadata beside the file, on the same storage provider.
 * This survives restarts and settings reloads without changing upstream bytes or their checksums.
 */
internal class MetadataStorage(private val storage: StorageProvider) {

    // Serialize metadata and origin updates with merge reads, without retaining a lock per artifact.
    private val locks = Array(64) { Any() }

    fun putFile(gav: Location, data: InputStream, mirrored: Boolean = false): Result<Unit, ErrorResponse> {
        if (gav.getSimpleName() != METADATA_FILE) {
            return storage.putFile(gav, data)
        }

        return synchronized(lock(gav)) {
            when {
                mirrored -> {
                    val content = data.use { it.readBytes() }
                    storage.putFile(gav, content.inputStream())
                        .flatMap { storage.putFile(originFile(gav), fingerprint(content).byteInputStream()) }
                }
                else -> storage.putFile(gav, data).flatMap { removeOrigin(gav) }
            }
        }
    }

    fun removeFile(gav: Location): Result<Unit, ErrorResponse> =
        synchronized(lock(gav)) {
            storage.removeFile(gav).flatMap {
                when (gav.getSimpleName()) {
                    METADATA_FILE -> removeOrigin(gav)
                    else -> Unit.asSuccess() // Recursive directory deletes already remove their sidecars.
                }
            }
        }

    fun readLocalMetadata(gav: Location): Result<List<ByteArray>, ErrorResponse> =
        synchronized(lock(gav)) {
            when {
                !storage.exists(gav) -> Result.ok(emptyList())
                else -> storage.getFile(gav).map { it.use { data -> data.readBytes() } }.flatMap { content ->
                    when {
                        !storage.exists(originFile(gav)) -> Result.ok(listOf(content))
                        else -> storage.getFile(originFile(gav)).map { origin ->
                            val mirroredFingerprint = origin.use { it.readBytes().decodeToString() }
                            if (fingerprint(content) == mirroredFingerprint) emptyList() else listOf(content)
                        }
                    }
                }
            }
        }

    private fun removeOrigin(gav: Location): Result<Unit, ErrorResponse> =
        when {
            storage.exists(originFile(gav)) -> storage.removeFile(originFile(gav))
            else -> Unit.asSuccess()
        }

    private fun originFile(gav: Location): Location =
        gav.resolveSibling("$METADATA_FILE.reposilite-origin")

    private fun fingerprint(content: ByteArray): String =
        Checksum.SHA256.generate(content.inputStream())

    private fun lock(gav: Location): Any =
        locks[Math.floorMod(gav.hashCode(), locks.size)]

}
