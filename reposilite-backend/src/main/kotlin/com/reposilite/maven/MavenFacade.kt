/*
 * Copyright (c) 2022 dzikoysk
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
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.VersionsResponse
import com.reposilite.plugin.api.Facade
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import java.io.InputStream

class MavenFacade internal constructor(
    private val journalist: Journalist,
    private val repositorySecurityProvider: RepositorySecurityProvider,
    private val repositoryService: RepositoryService,
    private val mavenService: MavenService,
    private val metadataService: MetadataService,
    private val latestService: LatestService,
) : Journalist, Facade {

    fun findDetails(lookupRequest: LookupRequest): Result<out FileDetails, ErrorResponse> =
        mavenService.findDetails(lookupRequest)

    fun findFile(lookupRequest: LookupRequest): Result<Pair<DocumentInfo, InputStream>, ErrorResponse> =
        mavenService.findFile(lookupRequest)

    fun deployFile(deployRequest: DeployRequest): Result<Unit, ErrorResponse> =
        mavenService.deployFile(deployRequest)

    fun deleteFile(deleteRequest: DeleteRequest): Result<Unit, ErrorResponse> =
        mavenService.deleteFile(deleteRequest)

    fun saveMetadata(saveMetadataRequest: SaveMetadataRequest): Result<Metadata, ErrorResponse> =
        metadataService.saveMetadata(saveMetadataRequest)

    fun generatePom(generatePomRequest: GeneratePomRequest): Result<Unit, ErrorResponse> =
        metadataService.generatePom(generatePomRequest)

    fun findMetadata(repository: Repository, gav: Location): Result<Metadata, ErrorResponse> =
        metadataService.findMetadata(repository, gav)

    fun findVersions(lookupRequest: VersionLookupRequest): Result<VersionsResponse, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.repository, lookupRequest.gav)
            .flatMap { metadataService.findVersions(lookupRequest.repository, lookupRequest.gav, lookupRequest.filter) }

    fun findLatestVersion(lookupRequest: VersionLookupRequest): Result<LatestVersionResponse, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, lookupRequest.repository, lookupRequest.gav)
            .flatMap { metadataService.findLatestVersion(lookupRequest.repository, lookupRequest.gav, lookupRequest.filter) }

    fun <T> findLatestVersionFile(latestArtifactQueryRequest: LatestArtifactQueryRequest, handler: MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        latestService.queryLatestArtifact(
            request = latestArtifactQueryRequest,
            supplier = { findLatestVersion(it) },
            handler = handler
        )

    fun createLatestBadge(lookupRequest: LatestBadgeRequest): Result<String, ErrorResponse> =
        findLatestVersion(lookupRequest.toVersionLookupRequest())
            .flatMap { latestService.createLatestBadge(lookupRequest, it.version) }

    fun canAccessResource(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<Unit, ErrorResponse> =
        repositorySecurityProvider.canAccessResource(accessToken, repository, gav)

    fun findRepositories(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        repositoryService.getRootDirectory(accessToken)

    fun getRepository(name: String) =
        repositoryService.getRepository(name)

    fun getRepositories(): Collection<Repository> =
        repositoryService.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}
