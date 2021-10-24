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

import com.reposilite.maven.api.FileDetails
import com.reposilite.settings.SharedConfiguration.RepositoryConfiguration.ProxiedHostConfiguration
import com.reposilite.shared.RemoteClient
import com.reposilite.shared.firstOrErrors
import com.reposilite.shared.toPath
import com.reposilite.web.http.ErrorResponse
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import panda.std.Result.ok
import java.io.InputStream
import java.nio.file.Path

internal class ProxyService(private val remoteClient: RemoteClient) {

    fun findRemoteDetails(repository: Repository, gav: String): Result<out FileDetails, ErrorResponse> =
        searchInRemoteRepositories(repository) { host, config ->
            remoteClient.head("$host/$gav", config.authorization, config.connectTimeout, config.readTimeout)
        }

    fun findRemoteFile(repository: Repository, gav: String): Result<out InputStream, ErrorResponse> =
        searchInRemoteRepositories(repository) { host, config ->
            remoteClient.get("$host/$gav", config.authorization, config.connectTimeout, config.readTimeout)
                .flatMap { data -> if (config.store) storeFile(repository, gav.toPath(), data) else ok(data) }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun <V> searchInRemoteRepositories(repository: Repository, fetch: (String, ProxiedHostConfiguration) -> Result<out V, ErrorResponse>): Result<out V, ErrorResponse> =
        repository.proxiedHosts.asSequence()
            .map { (host, config) -> fetch(host, config) }
            .firstOrErrors()
            .mapErr { errors -> ErrorResponse(NOT_FOUND, errors.joinToString(" -> ") { "(${it.status}: ${it.message})" }) }

    private fun storeFile(repository: Repository, gav: Path, data: InputStream): Result<InputStream, ErrorResponse> =
        repository
            .putFile(gav, data)
            .flatMap { repository.getFile(gav) }

}