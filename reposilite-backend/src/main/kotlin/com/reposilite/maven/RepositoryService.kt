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
                        .peek { if (gav.isResolutionMetadata()) repository.resolutionCache?.invalidate(gav) }
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
                        .peek { if (gav.isResolutionMetadata()) repository.resolutionCache?.invalidate(gav) }
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
            tryRemote = { hosts -> mirrorService.findRemoteFile(repository, gav, accessToken, hosts) },
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
            tryRemote = { hosts -> mirrorService.findRemoteDetails(repository, gav, accessToken, hosts) },
        ).peek {
            recordResolvedRequest(Identifier(repository.name, gav.toString()), it)
        }

    private inline fun <T : Any> resolveWithCache(
        repository: Repository,
        gav: Location,
        accessToken: AccessTokenIdentifier?,
        mirrorFirst: Boolean,
        crossinline tryLocal: () -> Result<T, ErrorResponse>,
        crossinline tryRemote: (hosts: List<MirrorHost>) -> MirrorResolution<T>,
    ): Result<T, ErrorResponse> {
        val cache = repository.resolutionCache
        val authenticated = accessToken != null
        val cached = cache?.lookup(gav, authenticated)
        val isMetadata = gav.isResolutionMetadata()
        val notFoundMessage = "Cannot find '$gav' in local or remote repositories"

        if (isMetadata && cached == ResolutionCache.Origin.Negative) {
            return notFoundError(notFoundMessage)
        }

        val hosts = (cached as? ResolutionCache.Origin.Remote)
            ?.let { decision -> repository.mirrorHosts.filter { it.host == decision.host }.ifEmpty { repository.mirrorHosts } }
            ?: repository.mirrorHosts

        val attempt = when {
            cached is ResolutionCache.Origin.Remote || mirrorFirst -> tryRemoteFirst(tryLocal, tryRemote, hosts, notFoundMessage)
            else -> tryLocalFirst(tryLocal, tryRemote, hosts, notFoundMessage)
        }

        if (cache != null && isMetadata) {
            attempt.recordTo(cache, gav, authenticated)
        }
        return attempt.result
    }

    private inline fun <T : Any> tryLocalFirst(
        tryLocal: () -> Result<T, ErrorResponse>,
        tryRemote: (List<MirrorHost>) -> MirrorResolution<T>,
        hosts: List<MirrorHost>,
        notFoundMessage: String,
    ): ResolveAttempt<T> {
        val local = tryLocal()
        return when {
            local.isOk -> ResolveAttempt(local, remote = null)
            else -> {
                val remote = tryRemote(hosts)
                ResolveAttempt(remote.toResult(notFoundMessage).flatMapErr { local }, remote)
            }
        }
    }

    private inline fun <T : Any> tryRemoteFirst(
        tryLocal: () -> Result<T, ErrorResponse>,
        tryRemote: (List<MirrorHost>) -> MirrorResolution<T>,
        hosts: List<MirrorHost>,
        notFoundMessage: String,
    ): ResolveAttempt<T> {
        val remote = tryRemote(hosts)
        return when (remote) {
            is MirrorResolution.Resolved -> ResolveAttempt(Result.ok(remote.value), remote)
            else -> ResolveAttempt(tryLocal().flatMapErr { remote.toResult(notFoundMessage) }, remote)
        }
    }

    private fun <T> ResolveAttempt<T>.recordTo(cache: ResolutionCache, gav: Location, authenticated: Boolean) {
        val prefix = gav.getParent()
        if (prefix.toString().isEmpty()) {
            return
        }
        val origin = when {
            result.isOk -> when (val r = remote) {
                is MirrorResolution.Resolved -> when {
                    r.mirror.configuration.store -> ResolutionCache.Origin.Local
                    else -> ResolutionCache.Origin.Remote(r.mirror.host)
                }
                else -> ResolutionCache.Origin.Local
            }
            remote is MirrorResolution.NotFound -> ResolutionCache.Origin.Negative
            else -> return // transient failure or no mirrors probed — don't poison cache
        }
        cache.record(prefix, authenticated, origin)
    }

    private data class ResolveAttempt<T>(
        val result: Result<T, ErrorResponse>,
        val remote: MirrorResolution<T>?, // null when remote was not attempted
    )

    private fun Location.isResolutionMetadata(): Boolean {
        val name = getSimpleName()
        return when {
            name.endsWith(".pom") -> true
            name.endsWith(".module") -> true
            name == METADATA_FILE -> true
            name.endsWith(".xml") -> name == "ivy.xml" || name.startsWith("ivy-")
            else -> false
        }
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
