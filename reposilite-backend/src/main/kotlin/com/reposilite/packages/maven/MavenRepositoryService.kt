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
package com.reposilite.packages.maven

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.packages.maven.api.DeleteRequest
import com.reposilite.packages.maven.api.DeployEvent
import com.reposilite.packages.maven.api.DeployRequest
import com.reposilite.packages.maven.api.Identifier
import com.reposilite.packages.maven.api.LookupRequest
import com.reposilite.packages.maven.api.PreResolveEvent
import com.reposilite.packages.maven.api.ResolvedDocument
import com.reposilite.packages.maven.api.ResolvedFileEvent
import com.reposilite.plugin.Extensions
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.errorResponse
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
import io.javalin.http.HttpStatus.CONFLICT
import panda.std.Result
import panda.std.asSuccess
import panda.std.ok
import java.io.InputStream

internal class MavenRepositoryService(
    private val journalist: Journalist,
    val mavenRepositoryProvider: MavenRepositoryProvider,
    private val securityProvider: MavenRepositorySecurityProvider,
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
                mavenRepository.acceptsDeploymentOf(gav) ->
                    mavenRepository.storageProvider
                        .putFile(gav, deployRequest.content)
                        .peek { logger.info("DEPLOY | Artifact $gav successfully deployed to ${mavenRepository.name} by ${deployRequest.by}") }
                        .peek { extensions.emitEvent(DeployEvent(mavenRepository, gav, deployRequest.by)) }
                        .flatMap { _ ->
                            when {
                                deployRequest.generateChecksums ->
                                    mavenRepository.storageProvider
                                        .getFile(gav)
                                        .peek { logger.info("DEPLOY | Generating checksums for $gav") }
                                        .flatMap { mavenRepository.writeFileChecksums(gav, it) }
                                else -> {
                                    logger.debug("DEPLOY | Skipping checksums generation for $gav")
                                    ok()
                                }
                            }
                        }
                else -> errorResponse(CONFLICT, "Redeployment is not allowed")
            }
        }

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> =
        with(deleteRequest) {
            when {
                securityProvider.canModifyResource(accessToken, mavenRepository, gav) ->
                    mavenRepository.storageProvider
                        .removeFile(gav)
                        .peek { logger.info("DELETE | File $gav has been deleted from ${mavenRepository.name} by ${deleteRequest.by}") }
                else -> unauthorizedError("Unauthorized access request")
            }
        }

    fun findDetails(lookupRequest: LookupRequest): Result<FileDetails, ErrorResponse> =
        resolve(lookupRequest) { repository, gav -> findDetails(lookupRequest.accessToken, repository, gav) }

    fun findFile(lookupRequest: LookupRequest): Result<ResolvedDocument, ErrorResponse> =
        resolve(lookupRequest) { repository, gav ->
            findFile(lookupRequest.accessToken, repository, gav).map { (details, stream) ->
                ResolvedDocument(
                    document = details,
                    cachable = repository.acceptsCachingOf(gav),
                    content = stream
                )
            }
        }

    private fun <T> resolve(lookupRequest: LookupRequest, block: (MavenRepository, Location) -> Result<T, ErrorResponse>): Result<T, ErrorResponse> {
        val (accessToken, repositoryName, gav) = lookupRequest
        val repository = mavenRepositoryProvider.getRepository(lookupRequest.repository) ?: return notFoundError("Repository $repositoryName not found")

        return canAccessResource(lookupRequest.accessToken, repository.name, gav)
            .onError { logger.debug("ACCESS | Unauthorized attempt of access (token: $accessToken) to $gav from ${repository.name}") }
            .peek { extensions.emitEvent(PreResolveEvent(accessToken, repository, gav)) }
            .flatMap { block(repository, gav) }
    }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: String, gav: Location): Result<Unit, ErrorResponse> =
        mavenRepositoryProvider.findRepository(repository)
            .flatMap { securityProvider.canAccessResource(accessToken, it, gav) }

    private fun findFile(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<Pair<DocumentInfo, InputStream>, ErrorResponse> =
        findDetails(accessToken, mavenRepository, gav)
            .`is`(DocumentInfo::class.java) { notFound("Requested file is a directory") }
            .flatMap { details -> findInputStream(mavenRepository, gav).map { details to it } }
            .let { extensions.emitEvent(ResolvedFileEvent(accessToken, mavenRepository, gav, it)).result }

    fun findInputStream(lookupRequest: LookupRequest): Result<InputStream, ErrorResponse> =
        resolve(lookupRequest) { repository, gav -> findInputStream(repository, gav) }

    private fun findInputStream(mavenRepository: MavenRepository, gav: Location): Result<InputStream, ErrorResponse> =
        when {
            mirrorService.shouldPrioritizeMirrorRepository(mavenRepository, gav) -> {
                logger.debug("Prioritizing mirror repository for '$gav'")
                mirrorService
                    .findRemoteFile(mavenRepository, gav)
                    .flatMapErr { mavenRepository.storageProvider.getFile(gav) }
            }
            mavenRepository.storageProvider.exists(gav) -> {
                logger.debug("Gav '$gav' found in '${mavenRepository.name}' repository")
                mavenRepository.storageProvider.getFile(gav)
            }
            else -> {
                logger.debug("Cannot find '$gav' in '${mavenRepository.name}' repository, requesting proxied repositories")
                mirrorService.findRemoteFile(mavenRepository, gav)
            }
        }

    private fun findDetails(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<FileDetails, ErrorResponse> =
        when {
            mirrorService.shouldPrioritizeMirrorRepository(mavenRepository, gav) -> {
                logger.debug("Prioritizing mirror repository for '$gav'")
                this
                    .findProxiedDetails(mavenRepository, gav)
                    .flatMapErr { findLocalDetails(accessToken, mavenRepository, gav) }
            }
            mavenRepository.storageProvider.exists(gav) -> findLocalDetails(accessToken, mavenRepository, gav) // todo: add fallback to local for shouldPrioritizeMirrorRepository
            else -> findProxiedDetails(mavenRepository, gav)
        }.peek {
            recordResolvedRequest(Identifier(mavenRepository.name, gav.toString()), it)
        }

    private fun findLocalDetails(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<FileDetails, ErrorResponse> =
        mavenRepository.storageProvider.getFileDetails(gav)
            .flatMap {
                it.takeIf { it.type == DIRECTORY }
                    ?.let { securityProvider.canBrowseResource(accessToken, mavenRepository, gav).map { _ -> it } }
                    ?: it.asSuccess()
            }

    private fun findProxiedDetails(mavenRepository: MavenRepository, gav: Location): Result<FileDetails, ErrorResponse> =
        mirrorService
            .findRemoteDetails(mavenRepository, gav)
            .mapErr { notFound("Cannot find '$gav' in local and remote repositories") }

    private fun recordResolvedRequest(identifier: Identifier, fileDetails: FileDetails) {
        if (fileDetails is DocumentInfo && ignoredExtensions.none { extension -> fileDetails.name.endsWith(extension) }) {
            statisticsFacade.incrementResolvedRequest(IncrementResolvedRequest(identifier))
        }
    }

    fun getRootDirectory(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        mavenRepositoryProvider.getRepositories()
            .filter { securityProvider.canAccessRepository(accessToken, it) }
            .map { SimpleDirectoryInfo(it.name) }
            .let { DirectoryInfo("/", it) }

    override fun getLogger(): Logger =
        journalist.logger

}
