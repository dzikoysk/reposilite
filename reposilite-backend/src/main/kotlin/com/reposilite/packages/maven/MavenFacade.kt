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
import com.reposilite.packages.maven.api.DeployRequest
import com.reposilite.packages.maven.api.GeneratePomRequest
import com.reposilite.packages.maven.api.LatestArtifactQueryRequest
import com.reposilite.packages.maven.api.LatestBadgeRequest
import com.reposilite.packages.maven.api.LatestVersionResponse
import com.reposilite.packages.maven.api.LookupRequest
import com.reposilite.packages.maven.api.Metadata
import com.reposilite.packages.maven.api.ResolvedDocument
import com.reposilite.packages.maven.api.SaveMetadataRequest
import com.reposilite.packages.maven.api.VersionLookupRequest
import com.reposilite.packages.maven.api.VersionsResponse
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
    private val mavenRepositorySecurityProvider: MavenRepositorySecurityProvider,
    private val mavenRepositoryProvider: MavenRepositoryProvider,
    private val metadataService: MetadataService,
    private val latestService: com.reposilite.packages.maven.LatestService,
) : Journalist, Facade {

    private val repositoryService = mavenRepositoryProvider.mavenRepositoryService

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

    fun findMetadata(mavenRepository: MavenRepository, gav: Location): Result<Metadata, ErrorResponse> =
        metadataService.findMetadata(mavenRepository, gav)

    fun findVersions(lookupRequest: VersionLookupRequest): Result<VersionsResponse, ErrorResponse> =
        mavenRepositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.mavenRepository, lookupRequest.gav)
            .flatMap { metadataService.findVersions(lookupRequest.mavenRepository, lookupRequest.gav, lookupRequest.filter) }

    fun findLatestVersion(lookupRequest: VersionLookupRequest): Result<LatestVersionResponse, ErrorResponse> =
        mavenRepositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.mavenRepository, lookupRequest.gav)
            .flatMap { metadataService.findLatestVersion(lookupRequest.mavenRepository, lookupRequest.gav, lookupRequest.filter) }

    fun <T> findLatestVersionFile(latestArtifactQueryRequest: LatestArtifactQueryRequest, handler: com.reposilite.packages.maven.MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        latestService.queryLatestArtifact(
            request = latestArtifactQueryRequest,
            supplier = { findLatestVersion(it) },
            handler = handler
        )

    fun getAvailableFiles(request: LookupRequest, directoryInfo: DirectoryInfo): List<FileDetails> =
        getRepository(request.repository)!!.let { repository ->
            directoryInfo.files.filter {
                mavenRepositorySecurityProvider.canBrowseResource(
                    accessToken = request.accessToken,
                    mavenRepository = repository,
                    gav = request.gav.resolve(it.name)
                ).isOk
            }
        }

    fun createLatestBadge(lookupRequest: LatestBadgeRequest): Result<String, ErrorResponse> =
        findLatestVersion(lookupRequest.toVersionLookupRequest())
            .flatMap { latestService.createLatestBadge(lookupRequest, it.version) }

    fun acceptsCachingOf(request: LookupRequest): Boolean =
        getRepository(request.repository)?.acceptsCachingOf(request.gav) ?: false

    fun canAccessResource(accessToken: AccessTokenIdentifier?, mavenRepository: MavenRepository, gav: Location): Result<Unit, ErrorResponse> =
        mavenRepositorySecurityProvider.canAccessResource(accessToken, mavenRepository, gav)

    fun findRepositories(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        repositoryService.getRootDirectory(accessToken)

    fun getRepository(name: String) =
        repositoryService.mavenRepositoryProvider.getRepository(name)

    fun getRepositories(): Collection<MavenRepository> =
        repositoryService.mavenRepositoryProvider.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}
