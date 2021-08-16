package com.reposilite.maven

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.shared.safeResolve
import com.reposilite.shared.toPath
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.NOT_ACCEPTABLE
import panda.std.Result

internal class MetadataService(
    private val repositoryService: RepositoryService
) {

    private val xml = XmlMapper(JacksonXmlModule().apply {
            setDefaultUseWrapper(false)
        })
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .enable(INDENT_OUTPUT)
        .registerKotlinModule()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun saveMetadata(repository: String, gav: String, metadata: Metadata) {
        repositoryService.findRepository(repository)
            .flatMap { it.putFile(gav.toPath().safeResolve(METADATA_FILE), xml.writeValueAsBytes(metadata)) }
    }

    fun findVersions(lookupRequest: LookupRequest): Result<Collection<String>, ErrorResponse> =
        repositoryService.findRepository(lookupRequest.repository)
            .flatMap { it.getFileDetails(lookupRequest.gav.toPath().safeResolve(METADATA_FILE)) }
            .filter({ it is DocumentInfo }, { ErrorResponse(NOT_ACCEPTABLE, "Maven metadata file cannot be directory") })
            .map { it as DocumentInfo }
            .map { xml.readValue<Metadata>(it.content()) }
            .map { it.versioning?.versions ?: emptyList() }
            .map { VersionComparator.sortStrings(it) }

    fun findLatest(lookupRequest: LookupRequest): Result<String, ErrorResponse> =
        findVersions(lookupRequest).map { it.last() }

}