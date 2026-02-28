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
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.GeneratePomRequest
import com.reposilite.maven.api.LatestArtifactQueryRequest
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.ResolvedDocument
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.VersionsResponse
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Result
import java.io.InputStream

class MavenFacade internal constructor(
    private val journalist: Journalist,
    private val repositorySecurityProvider: RepositorySecurityProvider,
    private val repositoryProvider: RepositoryProvider,
    private val metadataService: MetadataService,
    private val latestService: LatestService,
) : Journalist, Facade {

    private val repositoryService = repositoryProvider.repositoryService

    fun findDetails(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> =
        repositoryService.findDetails(lookupRequest)

    fun findFile(lookupRequest: LookupRequest): Result<ResolvedDocument, ErrorResponse> =
        repositoryService.findFile(lookupRequest)

    fun findData(lookupRequest: LookupRequest): Result<InputStream, ErrorResponse> =
        repositoryService.findInputStream(lookupRequest)

    fun deployFile(deployRequest: DeployRequest): Result<Unit, ErrorResponse> =
        repositoryService.deployFile(deployRequest)

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> =
        repositoryService.deleteFile(deleteRequest)

    fun saveMetadata(saveMetadataRequest: SaveMetadataRequest): Result<Metadata, ErrorResponse> =
        metadataService.saveMetadata(saveMetadataRequest)

    fun generatePom(generatePomRequest: GeneratePomRequest): Result<Unit, ErrorResponse> =
        metadataService.generatePom(generatePomRequest)

    fun findMetadata(repository: Repository, gav: Location): Result<Metadata, ErrorResponse> =
        metadataService.findMetadata(repository, gav)

    fun findVersions(lookupRequest: VersionLookupRequest): Result<VersionsResponse, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.repository, lookupRequest.gav)
            .flatMap { metadataService.findVersions(lookupRequest.repository, lookupRequest.gav, lookupRequest.filter, lookupRequest.sorted) }

    fun findLatestVersion(lookupRequest: VersionLookupRequest): Result<LatestVersionResponse, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.repository, lookupRequest.gav)
            .flatMap { metadataService.findLatestVersion(lookupRequest.repository, lookupRequest.gav, lookupRequest.filter, lookupRequest.sorted) }

    fun <T> findLatestVersionFile(latestArtifactQueryRequest: LatestArtifactQueryRequest, handler: MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        latestService.queryLatestArtifact(
            request = latestArtifactQueryRequest,
            supplier = { findLatestVersion(it) },
            handler = handler
        )

    fun getAvailableFiles(request: LookupRequest, directoryInfo: DirectoryInfo): List<FileDetails> =
        getRepository(request.repository)!!.let { repository ->
            directoryInfo.files.filter {
                repositorySecurityProvider.canBrowseResource(
                    accessToken = request.accessToken,
                    repository = repository,
                    gav = request.gav.resolve(it.name)
                ).isOk
            }
        }

    fun createLatestBadge(lookupRequest: LatestBadgeRequest): Result<String, ErrorResponse> =
        findLatestVersion(lookupRequest.toVersionLookupRequest())
            .flatMap { latestService.createLatestBadge(lookupRequest, it.version) }

    fun acceptsCachingOf(request: LookupRequest): Boolean =
        getRepository(request.repository)?.acceptsCachingOf(request.gav) ?: false

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<Unit, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(accessToken, repository, gav)

    fun findRepositories(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        repositoryService.getRootDirectory(accessToken)

    fun getRepository(name: String) =
        repositoryService.repositoryProvider.getRepository(name)

    fun getRepositories(): Collection<Repository> =
        repositoryService.repositoryProvider.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}
