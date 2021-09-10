/*
 * Copyright (c) 2021 dzikoysk
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

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.DirectoryInfo
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.notFound
import com.reposilite.shared.notFoundError
import com.reposilite.shared.toNormalizedPath
import com.reposilite.shared.toPath
import com.reposilite.shared.unauthorized
import com.reposilite.shared.unauthorizedError
import com.reposilite.token.api.AccessToken
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.BAD_REQUEST
import panda.std.Result
import panda.std.asError
import java.nio.file.Path
import java.nio.file.Paths

class MavenFacade internal constructor(
    private val journalist: Journalist,
    private val repositorySecurityProvider: RepositorySecurityProvider,
    private val repositoryService: RepositoryService,
    private val proxyService: ProxyService,
    private val metadataService: MetadataService
) : Journalist {

    companion object {
        val REPOSITORIES: Path = Paths.get("repositories")
    }

    suspend fun findFile(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> {
        val repository = repositoryService.getRepository(lookupRequest.repository) ?: return notFound("Repository not found").asError()
        val gav = lookupRequest.gav.toPath()

        if (repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, repository, gav).not()) {
            return unauthorized().asError()
        }

        if (repository.exists(gav).not()) {
            return proxyService.findFile(repository, lookupRequest.gav)
        }

        if (repository.isDirectory(gav) && repositorySecurityProvider.canBrowseResource(lookupRequest.accessToken, repository, gav).not()) {
            return unauthorizedError("Unauthorized indexing request")
        }

        return repository.getFileDetails(gav)
    }

    internal fun saveMetadata(repository: String, gav: String, metadata: Metadata): Result<Metadata, ErrorResponse> =
        metadataService.saveMetadata(repository, gav, metadata)

    fun findVersions(lookupRequest: LookupRequest): Result<List<String>, ErrorResponse> =
        repositoryService.findRepository(lookupRequest.repository)
            .filter({ repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, it, lookupRequest.gav.toPath())}, { unauthorized() })
            .flatMap { metadataService.findVersions(it, lookupRequest.gav) }

    fun findLatest(lookupRequest: LookupRequest): Result<String, ErrorResponse> =
        repositoryService.findRepository(lookupRequest.repository)
            .filter({ repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, it, lookupRequest.gav.toPath())}, { unauthorized() })
            .flatMap { metadataService.findLatest(it, lookupRequest.gav) }

    fun deployFile(deployRequest: DeployRequest): Result<DocumentInfo, ErrorResponse> {
        val repository = repositoryService.getRepository(deployRequest.repository) ?: return notFoundError("Repository not found")
        val path = deployRequest.gav.toNormalizedPath().orNull() ?: return errorResponse(BAD_REQUEST, "Invalid GAV")

        if (repository.redeployment.not() && path.getSimpleName().contains(METADATA_FILE).not() && repository.exists(path)) {
            return errorResponse(HttpCode.CONFLICT, "Redeployment is not allowed")
        }

        return repository.putFile(path, deployRequest.content)
            .peek { logger.info("DEPLOY Artifact successfully deployed $path by ${deployRequest.by}") }
    }

    fun deleteFile(deleteRequest: DeleteRequest): Result<*, ErrorResponse> {
        val repository = repositoryService.getRepository(deleteRequest.repository) ?: return notFoundError<Any>("Repository ${deleteRequest.repository} not found")
        val path = deleteRequest.gav.toNormalizedPath().orNull() ?: return notFoundError<Any>("Invalid GAV")

        if (repositorySecurityProvider.canModifyResource(deleteRequest.accessToken, repository, path).not()) {
            return unauthorizedError<Any>("Unauthorized access request")
        }

        return repository.removeFile(path)
    }

    fun findRepositories(accessToken: AccessToken?): DirectoryInfo =
        repositoryService.getRootDirectory(accessToken)

    fun getRepositories(): Collection<Repository> =
        repositoryService.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}