package com.reposilite.maven

import com.reposilite.config.Configuration.RepositoryConfiguration.ProxiedHostConfiguration
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.shared.RemoteClient
import com.reposilite.shared.toPath
import com.reposilite.web.coroutines.firstOrErrors
import com.reposilite.web.error.ErrorResponse
import com.reposilite.web.error.aggregatedError
import io.javalin.http.HttpCode.NOT_FOUND
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import panda.std.Result
import panda.std.Result.ok

internal class ProxyClient(private val remoteClient: RemoteClient) {

    suspend fun findFile(repository: Repository, gav: String): Result<out FileDetails, ErrorResponse> =
        repository.proxiedHosts
            .asSequence()
            .asFlow()
            .map { (host, configuration) -> findFile(repository, host, gav, configuration) }
            .firstOrErrors()
            .mapErr { aggregatedError(NOT_FOUND, it) }

    private suspend fun findFile(repository: Repository, host: String, gav: String, configuration: ProxiedHostConfiguration): Result<out FileDetails, ErrorResponse> =
        findFile(host, configuration, gav).flatMap { document ->
            if (configuration.store) storeFile(repository, gav, document) else ok<FileDetails, ErrorResponse>(document)
        }

    private fun storeFile(repository: Repository, gav: String, document: DocumentInfo): Result<out FileDetails, ErrorResponse> =
        repository
            .putFile(gav.toPath(), document.content())
            .flatMap { repository.getFileDetails(gav.toPath()) }

    private suspend fun findFile(host: String, configuration: ProxiedHostConfiguration, gav: String): Result<DocumentInfo, ErrorResponse> =
        remoteClient.get("$host/$gav", configuration.connectTimeout, configuration.readTimeout)
            .mapErr { error -> error.updateMessage { "$host: $it" } }

}