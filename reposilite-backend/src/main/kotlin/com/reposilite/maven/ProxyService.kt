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

import com.reposilite.config.Configuration.RepositoryConfiguration.ProxiedHostConfiguration
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.shared.RemoteClient
import com.reposilite.shared.toPath
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.NOT_FOUND
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import panda.std.Result
import panda.std.Result.ok
import panda.std.coroutines.firstOrErrors

internal class ProxyService(private val remoteClient: RemoteClient) {

    suspend fun findFile(repository: Repository, gav: String): Result<out FileDetails, ErrorResponse> =
        repository.proxiedHosts.asSequence()
            .asFlow()
            .map { (host, configuration) -> findFile(repository, host, gav, configuration) }
            .firstOrErrors()
            .mapErr { ErrorResponse(NOT_FOUND, it.joinToString("; ")) }

    private suspend fun findFile(repository: Repository, host: String, gav: String, configuration: ProxiedHostConfiguration): Result<out FileDetails, ErrorResponse> =
        findFile(host, configuration, gav).flatMap { document ->
            if (configuration.store)
                storeFile(repository, gav, document)
            else
                ok<FileDetails, ErrorResponse>(document)
        }

    private fun storeFile(repository: Repository, gav: String, document: DocumentInfo): Result<out FileDetails, ErrorResponse> =
        repository
            .putFile(gav.toPath(), document.content())
            .flatMap { repository.getFileDetails(gav.toPath()) }

    private suspend fun findFile(host: String, configuration: ProxiedHostConfiguration, gav: String): Result<DocumentInfo, ErrorResponse> =
        remoteClient.get("$host$gav", configuration.authorization, configuration.connectTimeout, configuration.readTimeout)
            .mapErr { error -> error.updateMessage { "$host: $it" } }

}