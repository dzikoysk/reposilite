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

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.shared.http.FakeRemoteClientProvider
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.toLocation
import com.reposilite.token.RoutePermission.WRITE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import panda.std.asSuccess

internal class MetadataOriginIntegrationTest : MavenSpecification() {

    private val gav = "test/artifact/maven-metadata.xml".toLocation()
    private val origin = gav.resolveSibling("maven-metadata.xml.reposilite-origin")
    private var upstream: String? = metadata("1.0")

    override val remoteClientProviderOverride: RemoteClientProvider = FakeRemoteClientProvider(
        headHandler = { _, _, _, _ -> notFoundError("Unused") },
        getHandler = { _, _, _, _ -> upstream?.byteInputStream()?.asSuccess() ?: notFoundError("Not found") },
    )

    override fun repositories() = listOf(RepositorySettings(
        id = "aggregate",
        proxied = listOf(MirroredRepositorySettings(reference = "https://upstream.example", store = true)),
    ))

    private fun metadata(version: String): String =
        "<metadata><groupId>test</groupId><artifactId>artifact</artifactId><versioning><versions><version>$version</version></versions></versioning></metadata>"

    private fun enableMerge() {
        mavenSettings.update(mavenSettings.get().copy(repositories = mavenSettings.get().repositories.map { it.copy(mergeMetadata = true) }))
    }

    private fun versions(): List<String>? =
        assertOk(mavenFacade.findData(LookupRequest(null, "aggregate", gav))).use {
            metadataXmlMapper.readValue<Metadata>(it).versioning?.versions
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should exclude previously mirrored metadata after recreating repositories`(mixedStorage: Boolean) {
        if (mixedStorage) {
            // Exercise the per-request fetch path as well as the all-storing single-flight path.
            mavenSettings.update(mavenSettings.get().copy(repositories = repositories().map {
                it.copy(proxied = it.proxied + MirroredRepositorySettings(reference = "https://fallback.example", store = false))
            }))
        }
        assertThat(versions()).containsExactly("1.0")
        val storage = mavenFacade.getRepository("aggregate")!!.storageProvider
        assertThat(assertOk(storage.getFile(gav)).use { it.readBytes().decodeToString() }).isEqualTo(upstream)
        assertThat(storage.exists(origin)).isTrue()

        upstream = metadata("2.0")
        enableMerge()

        assertThat(versions()).containsExactly("2.0")
        upstream = null
        assertThat(assertError(mavenFacade.findData(LookupRequest(null, "aggregate", gav))).status).isEqualTo(404)
    }

    @Test
    fun `should include a local deployment identical to previously cached metadata`() {
        assertThat(versions()).containsExactly("1.0")
        val repository = mavenFacade.getRepository("aggregate")!!
        assertOk(mavenFacade.deployFile(DeployRequest(repository, gav, "test", metadata("1.0").byteInputStream(), false)))
        assertThat(repository.storageProvider.exists(origin)).isFalse()
        upstream = metadata("2.0")
        enableMerge()

        assertThat(versions()).containsExactly("1.0", "2.0")
    }

    @Test
    fun `should include locally generated metadata identical to previously cached metadata`() {
        val local = metadataXmlMapper.readValue<Metadata>(metadata("1.0"))
        upstream = metadataXmlMapper.writeValueAsString(local)
        assertThat(versions()).containsExactly("1.0")
        val repository = mavenFacade.getRepository("aggregate")!!
        assertOk(mavenFacade.saveMetadata(SaveMetadataRequest(repository, gav, local)))
        assertThat(repository.storageProvider.exists(origin)).isFalse()
        upstream = metadata("2.0")
        enableMerge()

        assertThat(versions()).containsExactly("1.0", "2.0")
    }

    @Test
    fun `should preserve unmarked existing metadata when enabling merging`() {
        addFileToRepository(FileSpec("aggregate", gav.toString(), metadata("0.5")))
        enableMerge()

        assertThat(versions()).containsExactly("0.5", "1.0")
    }

    @Test
    fun `should ignore an outdated origin fingerprint after an external file replacement`() {
        assertThat(versions()).containsExactly("1.0")
        assertOk(mavenFacade.getRepository("aggregate")!!.storageProvider.putFile(gav, metadata("0.5").byteInputStream()))
        upstream = metadata("2.0")
        enableMerge()

        assertThat(versions()).containsExactly("0.5", "2.0")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should delete origin records with metadata or its directory`(directory: Boolean) {
        assertThat(versions()).containsExactly("1.0")
        val repository = mavenFacade.getRepository("aggregate")!!
        val token = createAccessToken("delete", "secret", "aggregate", "test", WRITE)

        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, if (directory) gav.getParent() else gav, "test")))

        assertThat(repository.storageProvider.exists(gav)).isFalse()
        assertThat(repository.storageProvider.exists(origin)).isFalse()
    }

}
