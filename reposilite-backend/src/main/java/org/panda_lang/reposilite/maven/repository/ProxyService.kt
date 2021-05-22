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
package org.panda_lang.reposilite.maven.repository

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.javanet.NetHttpTransport
import org.apache.http.HttpStatus
import org.panda_lang.reposilite.web.ReposiliteContext
import org.panda_lang.reposilite.ReposiliteException
import org.panda_lang.reposilite.failure.FailureFacade
import org.panda_lang.reposilite.failure.ResponseUtils
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.maven.repository.api.LookupResponse
import org.panda_lang.reposilite.storage.StorageProvider
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.function.Option
import org.panda_lang.utilities.commons.function.Result
import java.io.IOException
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture

internal class ProxyService(
    private val storeProxied: Boolean,
    private val proxyPrivate: Boolean,
    private val proxyConnectTimeout: Int,
    private val proxyReadTimeout: Int,
    private val proxied: List<String>,
    private val repositoryService: RepositoryService,
    private val failureFacade: FailureFacade,
    private val storageProvider: StorageProvider
) {

    private val httpRequestFactory: HttpRequestFactory = NetHttpTransport().createRequestFactory()

    fun findProxied(context: ReposiliteContext): Result<LookupResponse, ErrorResponse> {
        var uri = context.uri
        var repository = repositoryService.primaryRepository

        // remove repository name if defined
        for (localRepository in repositoryService.repositories) {
            if (uri.startsWith("/" + localRepository.name)) {
                repository = localRepository
                uri = uri.substring(1 + localRepository.name.length)
                break
            }
        }
        if (!proxyPrivate && repository.isPrivate()) {
            return Result.error<LookupResponse, ErrorResponse>(ErrorResponse(HttpStatus.SC_NOT_FOUND, "Proxying is disabled in private repositories"))
        }

        // /groupId/artifactId/<content>
        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error<LookupResponse, ErrorResponse>(ErrorResponse(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Invalid proxied request"))
        }

        val remoteUri = uri
        val list: MutableList<CompletableFuture<Void>> = ArrayList<CompletableFuture<Void>>()
        val responses = Collections.synchronizedList(ArrayList<HttpResponse>())

        for (proxied in proxied) {
            list.add(CompletableFuture.runAsync {
                try {
                    val remoteRequest = httpRequestFactory.buildGetRequest(GenericUrl(proxied + remoteUri))
                    remoteRequest.throwExceptionOnExecuteError = false
                    remoteRequest.connectTimeout = proxyConnectTimeout * 1000
                    remoteRequest.readTimeout = proxyReadTimeout * 1000
                    val remoteResponse = remoteRequest.execute()
                    val headers = remoteResponse.headers
                    if ("text/html" == headers.contentType) {
                        return@runAsync
                    }
                    if (remoteResponse.isSuccessStatusCode) {
                        responses.add(remoteResponse)
                    }
                } catch (exception: Exception) {
                    val message = "Proxied repository " + proxied + " is unavailable due to: " + exception.message
                    context.getLogger().error(message)
                    if (exception !is SocketTimeoutException) {
                        failureFacade.throwException(remoteUri, ReposiliteException(message, exception))
                    }
                }
            })
        }

        CompletableFuture.allOf(*list.toTypedArray<CompletableFuture<*>>()).join()

        return if (responses.isNotEmpty()) {
            val remoteResponse = responses[0]
            val contentLength = Option.of(remoteResponse.headers.contentLength).orElseGet(0L)
            val path = remoteUri.split("/").toTypedArray()
            val fileDetails = FileDetailsResponse(FileDetailsResponse.FILE, path.last(), "", remoteResponse.contentType, contentLength)
            val lookupResponse = LookupResponse.of(fileDetails)

            if (!context.method.equals("HEAD")) {
                if (storeProxied) {
                    return store(
                        remoteUri,
                        remoteResponse,
                        context
                    ).map { lookupResponse }
                }
                else {
                    context.output { outputStream: OutputStream ->
                        IOUtils.copyLarge(
                            remoteResponse.content,
                            outputStream
                        )
                    }
                }
            }

            Result.ok(lookupResponse)
        }
        else {
            ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Artifact $uri not found")
        }
    }

    private fun store(uri: String, remoteResponse: HttpResponse, context: ReposiliteContext): Result<FileDetailsResponse, ErrorResponse> {
        var uri = uri

        if (storageProvider.isFull()) {
            val error = "Not enough storage space available for $uri"
            context.getLogger().warn(error)
            return Result.error<FileDetailsResponse, ErrorResponse>(ErrorResponse(HttpStatus.SC_INSUFFICIENT_STORAGE, error))
        }

        val repositoryName = StringUtils.split(uri.substring(1), "/")[0] // skip first path separator
        val repository = repositoryService.getRepository(repositoryName)

        if (repository == null) {
            uri = repositoryService.primaryRepository.name + uri
        }

        val proxiedFile = Paths.get(uri)

        return try {
            val result: Result<FileDetailsResponse, ErrorResponse> = storageProvider.putFile(proxiedFile, remoteResponse.content)

            if (result.isOk) {
                context.logger.info("Stored proxied $proxiedFile from ${remoteResponse.request.url}")

                context.output { output: OutputStream ->
                    output.write(
                        storageProvider.getFile(proxiedFile).get()
                    )
                }
            }
            result
        }
        catch (ioException: IOException) {
            Result.error(ErrorResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Cannot process artifact"))
        }
    }

}