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
import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.ResolveEvent
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.VersionsResponse
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.BadgeGenerator
import com.reposilite.shared.extensions.`when`
import com.reposilite.shared.fs.DirectoryInfo
import com.reposilite.shared.fs.DocumentInfo
import com.reposilite.shared.fs.FileDetails
import com.reposilite.shared.fs.FileType.DIRECTORY
import com.reposilite.shared.fs.getSimpleName
import com.reposilite.shared.fs.toNormalizedPath
import com.reposilite.shared.fs.toPath
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.token.api.AccessToken
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFound
import com.reposilite.web.http.notFoundError
import com.reposilite.web.http.unauthorized
import com.reposilite.web.http.unauthorizedError
import io.javalin.http.HttpCode.BAD_REQUEST
import panda.std.Result
import panda.std.asError
import panda.std.reactive.Reference
import java.io.InputStream
import java.nio.file.Path

class MavenFacade internal constructor(
    private val journalist: Journalist,
    private val repositoryId: Reference<out String>,
    private val repositorySecurityProvider: RepositorySecurityProvider,
    private val repositoryService: RepositoryService,
    private val proxyService: ProxyService,
    private val metadataService: MetadataService,
    private val extensions: Extensions,
    private val statisticsFacade: StatisticsFacade
) : Journalist, Facade {

    private val ignoredExtensions = listOf(
        // Checksums
        ".md5",
        ".sha1",
        ".sha256",
        ".sha512",
        // Artifact descriptions
        ".pom",
        ".xml",
        // Artifact extensions
        "-sources.jar",
        "-javadoc.jar",
    )

    fun findDetails(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> =
        resolve(lookupRequest) { repository, gav ->
            if (repository.exists(gav).not()) {
                return@resolve proxyService.findRemoteDetails(repository, lookupRequest.gav)
            }

            val details = repository.getFileDetails(gav)

            if (details.`when` { it.type == DIRECTORY } && repositorySecurityProvider.canBrowseResource(lookupRequest.accessToken, repository, gav).not()) {
                return@resolve unauthorizedError("Unauthorized indexing request")
            }

            details.toOption()
                .`is`(DocumentInfo::class.java)
                .filter { ignoredExtensions.none { extension -> it.name.endsWith(extension) } }
                .peek { statisticsFacade.incrementResolvedRequest(IncrementResolvedRequest(lookupRequest.toIdentifier())) }

            details
        }

    fun findFile(lookupRequest: LookupRequest): Result<out InputStream, ErrorResponse> =
        resolve(lookupRequest) { repository, gav ->
            if (repository.exists(gav)) {
                logger.debug("Gav $gav found in ${repository.name} repository")
                repository.getFile(gav)
            } else {
                logger.debug("Cannot find $gav in ${repository.name} repository, requesting proxied repositories")
                proxyService.findRemoteFile(repository, lookupRequest.gav)
            }
        }

    private fun <T> resolve(lookupRequest: LookupRequest, block: (Repository, Path) -> Result<out T, ErrorResponse>): Result<out T, ErrorResponse> {
        val repository = repositoryService.getRepository(lookupRequest.repository) ?: return notFound("Repository not found").asError()
        val gav = lookupRequest.gav.toPath()

        if (repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, repository, gav).not()) {
            logger.debug("Unauthorized attempt of access (token: ${lookupRequest.accessToken}) to $gav from ${repository.name}")
            return unauthorized().asError()
        }

        extensions.emitEvent(ResolveEvent(lookupRequest, repository, gav))
        return block(repository, gav)
    }

    fun saveMetadata(repository: String, gav: String, metadata: Metadata): Result<Metadata, ErrorResponse> =
        metadataService.saveMetadata(repository, gav, metadata)

    fun findMetadata(repository: String, gav: String): Result<Metadata, ErrorResponse> =
        metadataService.findMetadata(repository, gav)

    fun findVersions(lookupRequest: VersionLookupRequest): Result<VersionsResponse, ErrorResponse> =
        repositoryService.findRepository(lookupRequest.repository)
            .filter({ repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, it, lookupRequest.gav.toPath())}, { unauthorized() })
            .flatMap { metadataService.findVersions(it, lookupRequest.gav, lookupRequest.filter) }

    fun findLatest(lookupRequest: VersionLookupRequest): Result<LatestVersionResponse, ErrorResponse> =
        repositoryService.findRepository(lookupRequest.repository)
            .filter({ repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, it, lookupRequest.gav.toPath())}, { unauthorized() })
            .flatMap { metadataService.findLatest(it, lookupRequest.gav, lookupRequest.filter) }

    fun findLatestBadge(request: LatestBadgeRequest): Result<String, ErrorResponse> =
        findLatest(VersionLookupRequest(null, request.repository, request.gav, request.filter))
            .flatMap { BadgeGenerator.generateSvg(request.name ?: repositoryId.get(), (request.prefix ?: "") + it.version, request.color) }

    fun deployFile(deployRequest: DeployRequest): Result<Unit, ErrorResponse> {
        val repository = repositoryService.getRepository(deployRequest.repository) ?: return notFoundError("Repository not found")
        val path = deployRequest.gav.toNormalizedPath().orNull() ?: return errorResponse(BAD_REQUEST, "Invalid GAV")

        if (repository.redeployment.not() && path.getSimpleName().contains(METADATA_FILE).not() && repository.exists(path)) {
            return errorResponse(BAD_REQUEST, "Redeployment is not allowed")
        }

        return repository.putFile(path, deployRequest.content)
            .peek { logger.info("DEPLOY | Artifact $path successfully deployed to ${repository.name} by ${deployRequest.by}") }
            .peek { extensions.emitEvent(DeployEvent(repository, path, deployRequest.by)) }
    }

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> {
        val repository = repositoryService.getRepository(deleteRequest.repository) ?: return notFoundError("Repository ${deleteRequest.repository} not found")
        val path = deleteRequest.gav.toNormalizedPath().orNull() ?: return notFoundError("Invalid GAV")

        if (repositorySecurityProvider.canModifyResource(deleteRequest.accessToken, repository, path).not()) {
            return unauthorizedError("Unauthorized access request")
        }

        return repository.removeFile(path)
    }

    fun findRepositories(accessToken: AccessToken?): DirectoryInfo =
        repositoryService.getRootDirectory(accessToken)

    fun getRepository(name: String) =
        repositoryService.getRepository(name)

    internal fun getRepositories(): Collection<Repository> =
        repositoryService.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}