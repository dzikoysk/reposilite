/*
 * Copyright (c) 2020-2026 dzikoysk
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

package com.reposilite.generic

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.plugin.api.Facade
import com.reposilite.repository.RepositoryFacade
import com.reposilite.repository.api.Repository as RepositoryApi
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.errorResponse
import com.reposilite.shared.unauthorizedError
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.HttpStatus.CONFLICT
import panda.std.Result
import panda.std.asSuccess
import panda.std.reactive.Reference
import java.io.InputStream

class GenericFacade internal constructor(
    private val journalist: Journalist,
    private val repositoryStore: GenericRepositoryStore,
    private val repositoryFacade: RepositoryFacade,
) : Facade, Journalist {

    fun getRepository(name: String): GenericRepository? =
        repositoryStore.findRepository(name)

    internal fun repositories(): Reference<Collection<RepositoryApi>> =
        repositoryStore.repositories()

    fun findDetails(
        accessToken: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
    ): Result<out FileDetails, ErrorResponse> =
        repositoryFacade.canAccessResource(accessToken, repository, location)
            .flatMap { repository.storageProvider.getFileDetails(location) }
            .flatMap { details ->
                if (details.type == DIRECTORY) {
                    repositoryFacade.canBrowseResource(accessToken, repository, location).map { details }
                } else {
                    details.asSuccess()
                }
            }

    fun findData(
        accessToken: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
    ): Result<InputStream, ErrorResponse> =
        repositoryFacade.canAccessResource(accessToken, repository, location)
            .flatMap { repository.storageProvider.getFile(location) }

    fun getAvailableFiles(
        accessToken: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
        directory: DirectoryInfo,
    ): List<FileDetails> =
        directory.files.filter { child ->
            repositoryFacade.canBrowseResource(accessToken, repository, location.resolve(child.name)).isOk
        }

    fun deployFile(
        accessToken: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
        content: InputStream,
        by: String,
    ): Result<Unit, ErrorResponse> =
        when {
            !repositoryFacade.canModifyResource(accessToken, repository, location) ->
                unauthorizedError("Unauthorized access request")
            !repository.redeployment && repository.storageProvider.exists(location) ->
                errorResponse(CONFLICT, "Redeployment is not allowed")
            else -> repository.storageProvider.putFile(location, content)
                .peek { logger.info("DEPLOY | File $location successfully deployed to ${repository.name} by $by") }
        }

    fun deleteFile(
        accessToken: AccessTokenIdentifier?,
        repository: GenericRepository,
        location: Location,
        by: String,
    ): Result<Unit, ErrorResponse> =
        when {
            repositoryFacade.canModifyResource(accessToken, repository, location) ->
                repository.storageProvider.removeFile(location)
                    .peek { logger.info("DELETE | File $location has been deleted from ${repository.name} by $by") }
            else -> unauthorizedError("Unauthorized access request")
        }

    override fun getLogger(): Logger =
        journalist.logger
}
