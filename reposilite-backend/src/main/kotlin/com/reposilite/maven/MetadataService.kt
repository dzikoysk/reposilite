/*
 * Copyright (c) 2022 dzikoysk
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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.VersionsResponse
import com.reposilite.storage.VersionComparator
import com.reposilite.storage.api.Location
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFound
import panda.std.Result
import panda.std.letIf

internal class MetadataService(
    private val repositoryService: RepositoryService
) {

    private val xml = XmlMapper.xmlBuilder()
        .addModules(JacksonXmlModule(), kotlinModule())
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .defaultUseWrapper(false)
        .enable(INDENT_OUTPUT)
        .build()

    fun saveMetadata(repository: String, gav: Location, metadata: Metadata): Result<Metadata, ErrorResponse> =
        repositoryService.findRepository(repository)
            .flatMap {
                val content = xml.writeValueAsBytes(metadata)
                val metadataFile = gav.resolve(METADATA_FILE)

                it.putFile(metadataFile, content.inputStream())
                    .flatMap { _ -> it.writeFileChecksums(metadataFile, content) }
            }
            .map { metadata }

    fun findMetadata(repository: String, gav: Location): Result<Metadata, ErrorResponse> =
        repositoryService.findRepository(repository)
            .flatMap { it.getFile(gav.resolve(METADATA_FILE)) }
            .map { it.use { data -> xml.readValue<Metadata>(data) } }

    fun findLatest(repository: Repository, gav: Location, filter: String?): Result<LatestVersionResponse, ErrorResponse> =
        findVersions(repository, gav, filter)
            .filter({ it.versions.isNotEmpty() }, { notFound("Given artifact does not have any declared version") })
            .map { (isSnapshot, versions) -> LatestVersionResponse(isSnapshot, versions.last()) }

    fun findVersions(repository: Repository, gav: Location, filter: String?): Result<VersionsResponse, ErrorResponse> =
        repository.getFile(gav.resolve(METADATA_FILE))
            .map { it.use { data -> xml.readValue<Metadata>(data) } }
            .map { extractVersions(it) }
            .map { (isSnapshot, versions) ->
                versions
                    .letIf(filter != null) { it.filter { version -> version.startsWith(filter!!) } }
                    .let { VersionComparator.sortStrings(it) }
                    .let { VersionsResponse(isSnapshot, it.toList()) }
            }

    private data class VersionSequence(val isSnapshot: Boolean = false, val versions: Sequence<String>)

    private fun extractVersions(metadata: Metadata): VersionSequence =
        (metadata.versioning?.versions?.asSequence()?.let { VersionSequence(false, it) }
            ?: metadata.versioning?.snapshotVersions?.asSequence()?.map { it.value }?.filterNotNull()?.let { VersionSequence(true, it) })
            ?: VersionSequence(false, emptySequence())

}