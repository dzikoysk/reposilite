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
import com.reposilite.maven.application.ProxiedRepository
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.Location
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.notFoundError
import panda.std.Result
import panda.std.Result.ok
import java.io.InputStream

internal class ProxyService(private val journalist: Journalist): Journalist {

    fun findRemoteDetails(repository: Repository, gav: Location): Result<out FileDetails, ErrorResponse> =
        searchInRemoteRepositories(repository, gav) { (host, config, client) ->
            client.head("$host/$gav", config.authorization?.toCredentials(), config.connectTimeout, config.readTimeout)
        }

    fun findRemoteFile(repository: Repository, gav: Location): Result<InputStream, ErrorResponse> =
        searchInRemoteRepositories(repository, gav) { (host, config, client) ->
            client.get("$host/$gav", config.authorization?.toCredentials(), config.connectTimeout, config.readTimeout)
                .flatMap { data -> if (config.store) storeFile(repository, gav, data) else ok(data) }
                .mapErr { error -> error.updateMessage { "$host: $it" } }
        }

    private fun <V> searchInRemoteRepositories(repository: Repository, gav: Location, fetch: (ProxiedHost) -> Result<V, ErrorResponse>): Result<V, ErrorResponse> =
        repository.proxiedHosts.asSequence()
            .filter {(_, config) -> isAllowed(config, gav) }
            .map { fetch(it) }
            .firstOrNull { it.isOk }
            ?: notFoundError("Cannot find '$gav' in remote repositories")

    private fun isAllowed(config: ProxiedRepository, gav: Location): Boolean =
        config.allowedGroups.isEmpty() ||
                config.allowedGroups
                    .map { it.replace('.', '/') }
                    .any { gav.toString().startsWith(it) }

    private fun storeFile(repository: Repository, gav: Location, data: InputStream): Result<InputStream, ErrorResponse> =
        repository
            .putFile(gav, data)
            .flatMap { repository.getFile(gav) }

    override fun getLogger(): Logger =
        journalist.logger

}
