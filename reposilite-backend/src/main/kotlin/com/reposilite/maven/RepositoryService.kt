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
import com.reposilite.maven.api.ResolvedDocument
import com.reposilite.maven.api.ResolvedFileDataEvent
import com.reposilite.maven.api.ResolvedFileEvent
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
import com.reposilite.token.api.AccessTokenDetails
import io.javalin.http.HttpStatus.CONFLICT
import panda.std.Result
import panda.std.asSuccess
import panda.std.ok
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

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
                        .peek { repository.resolutionCache?.invalidate(gav) }
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
                else -> errorResponse(CONFLICT, "Redeployment is not allowed")
            }
        }

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> =
        with(deleteRequest) {
            when {
                securityProvider.canModifyResource(accessToken, repository, gav) ->
                    repository.storageProvider
                        .removeFile(gav)
                        .peek { logger.info("DELETE | File $gav has been deleted from ${repository.name} by ${deleteRequest.by}") }
                        .peek { repository.resolutionCache?.invalidate(gav) }
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
            .flatMap { details -> findInputStream(repository, gav, accessToken).map { details to it } }
            .let { extensions.emitEvent(ResolvedFileEvent(accessToken, repository, gav, it)).result }

    fun findInputStream(lookupRequest: LookupRequest): Result<InputStream, ErrorResponse> =
        resolve(lookupRequest) { repository, gav -> findInputStream(repository, gav, lookupRequest.accessToken) }

    private fun findInputStream(repository: Repository, gav: Location, accessToken: AccessTokenIdentifier? = null): Result<InputStream, ErrorResponse> {
        val result = resolveWithCache(
            repository = repository,
            gav = gav,
            accessToken = accessToken,
            mirrorFirst = mirrorService.shouldPrioritizeMirrorRepository(repository, gav),
            tryLocal = { repository.storageProvider.getFile(gav) },
            tryRemote = { hosts, outcome -> mirrorService.findRemoteFile(repository, gav, accessToken, hosts, outcome) },
        )
        return extensions.emitEvent(ResolvedFileDataEvent(accessToken, repository, gav, result)).result
    }

    private fun findDetails(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<FileDetails, ErrorResponse> =
        resolveWithCache(
            repository = repository,
            gav = gav,
            accessToken = accessToken,
            mirrorFirst = mirrorService.shouldPrioritizeMirrorRepository(repository, gav),
            tryLocal = { findLocalDetails(accessToken, repository, gav) },
            tryRemote = { hosts, outcome ->
                mirrorService.findRemoteDetails(repository, gav, accessToken, hosts, outcome)
                    .mapErr { notFound("Cannot find '$gav' in local and remote repositories") }
            },
        ).peek {
            recordResolvedRequest(Identifier(repository.name, gav.toString()), it)
        }

    private inline fun <T : Any> resolveWithCache(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier?,
        mirrorFirst: Boolean,
        crossinline tryLocal: () -> Result<T, ErrorResponse>,
        crossinline tryRemote: (hosts: List<MirrorHost>, outcome: AtomicReference<MirrorProbeOutcome>) -> Result<T, ErrorResponse>,
    ): Result<T, ErrorResponse> {
        val cache = repository.resolutionCache
        val authenticated = accessToken != null
        val cached = cache?.lookup(gav, authenticated)

        if (cached == ResolutionCache.Origin.Negative) {
            return notFoundError("Cannot find '$gav' in local or remote repositories")
        }

        val hosts = (cached as? ResolutionCache.Origin.Remote)
            ?.let { decision -> repository.mirrorHosts.filter { it.host == decision.host }.ifEmpty { repository.mirrorHosts } }
            ?: repository.mirrorHosts

        val localFirst = when (cached) {
            ResolutionCache.Origin.Local -> true
            is ResolutionCache.Origin.Remote -> false
            null -> !mirrorFirst
            ResolutionCache.Origin.Negative -> error("unreachable")
        }

        val outcome = AtomicReference(MirrorProbeOutcome.UNDETERMINED)
        var localAttempted = false
        var localResolved = false
        var remoteAttempted = false

        val runLocal: () -> Result<T, ErrorResponse> = {
            localAttempted = true
            tryLocal().also { if (it.isOk) localResolved = true }
        }
        val runRemote: () -> Result<T, ErrorResponse> = {
            remoteAttempted = true
            tryRemote(hosts, outcome)
        }

        val result =
            if (localFirst) runLocal().flatMapErr { runRemote() }
            else runRemote().flatMapErr { runLocal() }

        if (cache != null && gav.getSimpleName().contains(METADATA_FILE)) {
            val prefix = gav.getParent()
            if (prefix.toString().isNotEmpty()) {
                val probe = outcome.get()
                // Negative requires confirmation from both sides — anything less risks pinning a transient outage as a permanent miss.
                when {
                    localResolved ->
                        cache.record(prefix, authenticated, ResolutionCache.Origin.Local)
                    probe.winningHost != null ->
                        cache.record(prefix, authenticated, ResolutionCache.Origin.Remote(probe.winningHost.host))
                    localAttempted && remoteAttempted && probe.allMissing ->
                        cache.record(prefix, authenticated, ResolutionCache.Origin.Negative)
                }
            }
        }

        return result
    }

    private fun findLocalDetails(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<FileDetails, ErrorResponse> =
        repository.storageProvider.getFileDetails(gav)
            .flatMap {
                it.takeIf { it.type == DIRECTORY }
                    ?.let { securityProvider.canBrowseResource(accessToken, repository, gav).map { _ -> it } }
                    ?: it.asSuccess()
            }

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
