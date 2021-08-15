package com.reposilite.maven

import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.LookupRequest
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result

internal class MetadataService(
    private val repositoryService: RepositoryService
) {

    fun generateMetadata() {}

    suspend fun findVersions(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> {
        val repository = repositoryService.getRepository(lookupRequest.repository) ?: return errorResponse(NOT_FOUND, "Repository not found")
        return errorResponse(NOT_FOUND, "Not implemented")
    }

    suspend fun findLatest(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> {
        val repository = repositoryService.getRepository(lookupRequest.repository) ?: return errorResponse(NOT_FOUND, "Repository not found")
        return errorResponse(NOT_FOUND, "Not implemented")
    }

}