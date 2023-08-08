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
package com.reposilite.maven

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.Identifier
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.PreResolveEvent
import com.reposilite.maven.api.ResolvedFileEvent
import com.reposilite.plugin.Extensions
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.notFound
import com.reposilite.shared.notFoundError
import com.reposilite.shared.unauthorizedError
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.IncrementResolvedRequest
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.SimpleDirectoryInfo
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Result
import panda.std.asSuccess
import panda.std.ok
import java.io.InputStream

internal class RepositoryService(
    private val journalist: Journalist,
    val repositoryProvider: RepositoryProvider,
    private val securityProvider: RepositorySecurityProvider,
    private val mirrorService: MirrorService,
    private val statisticsFacade: StatisticsFacade,
    private val extensions: Extensions,
) : Journalist {

    private val ignoredExtensions = listOf(
        // Checksums
        ".md5",
        ".sha1",
        ".sha256",
        ".sha512",
        // Artifact descriptions
        ".pom",
        ".xml",
        ".module",
        // Artifact extensions
        "-sources.jar",
        "-javadoc.jar",
        // Sign
        ".asc"
    )

    fun deployFile(deployRequest: DeployRequest): Result<Unit, ErrorResponse> =
        with(deployRequest) {
            when {
                repository.acceptsDeploymentOf(gav) ->
                    repository.storageProvider
                        .putFile(gav, deployRequest.content)
                        .peek { logger.info("DEPLOY | Artifact $gav successfully deployed to ${repository.name} by ${deployRequest.by}") }
                        .peek { extensions.emitEvent(DeployEvent(repository, gav, deployRequest.by)) }
                        .flatMap { _ ->
                            when {
                                deployRequest.generateChecksums ->
                                    repository.storageProvider
                                        .getFile(gav)
                                        .peek { logger.info("DEPLOY | Generating checksums for $gav") }
                                        .flatMap { repository.writeFileChecksums(gav, it) }
                                else -> {
                                    logger.debug("DEPLOY | Skipping checksums generation for $gav")
                                    ok()
                                }
                            }
                        }
                else -> badRequestError("Redeployment is not allowed")
            }
        }

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> =
        with(deleteRequest) {
            when {
                securityProvider.canModifyResource(accessToken, repository, gav) ->
                    repository.storageProvider
                        .removeFile(gav)
                        .peek { logger.info("DELETE | File $gav has been deleted from ${repository.name} by ${deleteRequest.by}") }
                else -> unauthorizedError("Unauthorized access request")
            }
        }

    fun findDetails(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> =
        resolve(lookupRequest) { repository, gav -> findDetails(lookupRequest.accessToken, repository, gav) }

    fun findFile(lookupRequest: LookupRequest): Result<Pair<DocumentInfo, InputStream>, ErrorResponse> =
        resolve(lookupRequest) { repository, gav -> findFile(lookupRequest.accessToken, repository, gav) }

    private fun <T> resolve(lookupRequest: LookupRequest, block: (Repository, Location) -> Result<T, ErrorResponse>): Result<T, ErrorResponse> {
        val (accessToken, repositoryName, gav) = lookupRequest
        val repository = repositoryProvider.getRepository(lookupRequest.repository) ?: return notFoundError("Repository $repositoryName not found")

        return canAccessResource(lookupRequest.accessToken, repository.name, gav)
            .onError { logger.debug("ACCESS | Unauthorized attempt of access (token: $accessToken) to $gav from ${repository.name}") }
            .peek { extensions.emitEvent(PreResolveEvent(accessToken, repository, gav)) }
            .flatMap { block(repository, gav) }
    }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: String, gav: Location): Result<Unit, ErrorResponse> =
        repositoryProvider.findRepository(repository)
            .flatMap { securityProvider.canAccessResource(accessToken, it, gav) }

    private fun findFile(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<Pair<DocumentInfo, InputStream>, ErrorResponse> =
        findDetails(accessToken, repository, gav)
            .`is`(DocumentInfo::class.java) { notFound("Requested file is a directory") }
            .flatMap { details -> findInputStream(repository, gav).map { details to it } }
            .let { extensions.emitEvent(ResolvedFileEvent(accessToken, repository, gav, it)).result }

    private fun findInputStream(repository: Repository, gav: Location): Result<InputStream, ErrorResponse> =
        if (repository.storageProvider.exists(gav)) {
            logger.debug("Gav '$gav' found in '${repository.name}' repository")
            repository.storageProvider.getFile(gav)
        } else {
            logger.debug("Cannot find '$gav' in '${repository.name}' repository, requesting proxied repositories")
            mirrorService.findRemoteFile(repository, gav)
        }

    private fun findDetails(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<out FileDetails, ErrorResponse> =
        when {
            repository.storageProvider.exists(gav) -> findLocalDetails(accessToken, repository, gav)
            else -> findProxiedDetails(repository, gav)
        }.peek {
            recordResolvedRequest(Identifier(repository.name, gav.toString()), it)
        }

    private fun findLocalDetails(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<out FileDetails, ErrorResponse> =
        repository.storageProvider.getFileDetails(gav)
            .flatMap {
                it.takeIf { it.type == DIRECTORY }
                    ?.let { securityProvider.canBrowseResource(accessToken, repository, gav).map { _ -> it } }
                    ?: it.asSuccess()
            }

    private fun findProxiedDetails(repository: Repository, gav: Location): Result<out FileDetails, ErrorResponse> =
        mirrorService
            .findRemoteDetails(repository, gav)
            .mapErr { notFound("Cannot find '$gav' in local and remote repositories") }

    private fun recordResolvedRequest(identifier: Identifier, fileDetails: FileDetails) {
        if (fileDetails is DocumentInfo && ignoredExtensions.none { extension -> fileDetails.name.endsWith(extension) }) {
            statisticsFacade.incrementResolvedRequest(IncrementResolvedRequest(identifier))
        }
    }

    fun getRootDirectory(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        repositoryProvider.getRepositories()
            .filter { securityProvider.canAccessRepository(accessToken, it) }
            .map { SimpleDirectoryInfo(it.name) }
            .let { DirectoryInfo("/", it) }

    override fun getLogger(): Logger =
        journalist.logger

}
