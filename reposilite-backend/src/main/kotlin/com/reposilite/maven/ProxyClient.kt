package com.reposilite.maven

import com.reposilite.config.Configuration.RepositoryConfiguration.ProxyConfiguration
import com.reposilite.failure.api.ErrorResponse
import com.reposilite.failure.api.aggregatedError
import com.reposilite.shared.RemoteClient
import com.reposilite.web.firstOrErrors
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class ProxyClient(
    private val hosts: Map<String, ProxyConfiguration>,
    private val ioExecutor: ExecutorService,
    private val remoteClient: RemoteClient
) {

    fun findFile(gav: String): Future<Result<InputStream, ErrorResponse>> =
        submit {
            hosts.asSequence()
                .map { (host, configuration) -> findFile(host, configuration, gav) }
                .firstOrErrors()
                .mapErr { aggregatedError(NOT_FOUND, it) }
        }

    private fun findFile(host: String, configuration: ProxyConfiguration, gav: String): Result<InputStream, ErrorResponse> =
        remoteClient.get("$host/$gav", configuration.connectTimeout, configuration.readTimeout)
            .mapErr { error -> error.updateMessage { "$host: $it" } }

    private fun <T> submit(callable: Callable<T>): Future<T> =
        ioExecutor.submit(callable)

}