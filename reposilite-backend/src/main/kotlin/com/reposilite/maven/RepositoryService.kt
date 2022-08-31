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
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.SimpleDirectoryInfo
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Result
import panda.std.asSuccess

internal class RepositoryService(
    private val journalist: Journalist,
    private val repositoryProvider: RepositoryProvider,
    private val securityProvider: RepositorySecurityProvider
) : Journalist {

    fun findRepository(name: String): Result<Repository, ErrorResponse> =
        getRepository(name)
            ?.asSuccess()
            ?: notFoundError("Repository $name not found")

    fun getRepository(name: String): Repository? =
        repositoryProvider.getRepositories()[name]

    fun getRootDirectory(accessToken: AccessTokenIdentifier?): DirectoryInfo =
        getRepositories()
            .filter { securityProvider.canAccessRepository(accessToken, it) }
            .map { SimpleDirectoryInfo(it.name) }
            .let { DirectoryInfo("/", it) }

    fun getRepositories(): Collection<Repository> =
        repositoryProvider.getRepositories().values

    override fun getLogger(): Logger =
        journalist.logger

}
