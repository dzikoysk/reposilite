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

package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.Source
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extensions.encoding
import com.reposilite.shared.notFoundError
import com.reposilite.status.FailureFacade
import com.reposilite.storage.api.toLocation
import com.reposilite.storage.getExtension
import com.reposilite.storage.getSimpleName
import com.reposilite.storage.inputStream
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence
import panda.std.Result
import panda.std.asSuccess

internal sealed class FrontendHandler(
    private val frontendFacade: FrontendFacade,
    private val failureFacade: FailureFacade,
) : ReposiliteRoutes() {

    protected fun respondWithResource(ctx: Context, uri: String, source: Source): Result<InputStream, ErrorResponse> {
        val contentType = ContentType.getContentTypeByExtension(uri.getExtension())
        ctx.contentType(contentType?.mimeType ?: ContentType.OCTET_STREAM)

        return when (uri.contains(".html") || uri.contains(".js")) {
            true -> respondWithProcessedResource(ctx, uri, source)
            else -> respondWithRawResource(source)
        }
    }

    private fun respondWithProcessedResource(ctx: Context, uri: String, source: Source): Result<InputStream, ErrorResponse> =
        frontendFacade.resolve(uri) { source.get() }
            ?.let { resource ->
                resource
                    .supply()
                    .peek { ctx.encoding(UTF_8) }
                    .onError { failureFacade.throwException(uri, it) }
                    .mapErr { ErrorResponse(INTERNAL_SERVER_ERROR, "Cannot serve resource") }
            }
            ?: notFoundError("Resource not found")

    private fun respondWithRawResource(source: Source): Result<InputStream, ErrorResponse> =
        source.get()
            ?.asSuccess()
            ?: notFoundError("Resource not found")

}

internal class ResourcesFrontendHandler(
    failureFacade: FailureFacade,
    frontendFacade: FrontendFacade,
    private val resourcesDirectory: String
) : FrontendHandler(frontendFacade, failureFacade) {

    private val defaultHandler = ReposiliteRoute<InputStream>("/", GET) {
        response = respondWithBundledResource(ctx, "index.html")
    }

    private val indexHandler = ReposiliteRoute<InputStream>("/index.html", GET) {
        response = respondWithBundledResource(ctx, "index.html")
    }

    private val assetsHandler = ReposiliteRoute<InputStream>("/assets/<path>", GET) {
        response = respondWithBundledResource(ctx, "assets/${ctx.pathParam("path")}")
    }

    private fun respondWithBundledResource(ctx: Context, uri: String): Result<InputStream, ErrorResponse> =
        respondWithResource(ctx, uri) {
            FrontendFacade::class.java.getResourceAsStream("/$resourcesDirectory/$uri") ?: "".toByteArray().inputStream()
        }

    override val routes = routes(defaultHandler, indexHandler, assetsHandler)

}

internal class CustomFrontendHandler(
    failureFacade: FailureFacade,
    frontendFacade: FrontendFacade,
    directory: Path
) : FrontendHandler(frontendFacade, failureFacade) {

    private fun rootFileHandler(file: Path) = ReposiliteRoute<InputStream>("/${file.getSimpleName()}", GET) {
        response = respondWithResource(ctx, file.getSimpleName()) {
            file.inputStream().orNull()
        }
    }

    private fun directoryHandler(directory: Path) = ReposiliteRoute<InputStream>("/${directory.fileName}/<path>", GET) {
        response = respondWithResource(ctx, directory.getSimpleName()) {
            parameter("path")
                .toLocation()
                .toPath()
                .map { path -> directory.resolve(path) }
                .flatMap { path -> path.inputStream().mapErr { error -> error.message } }
                .orNull()
        }
    }

    private fun indexHandler(directory: Path) = ReposiliteRoute<InputStream>("/", GET) {
        response = respondWithResource(ctx, "index.html") {
            directory.resolve("index.html")
                .inputStream()
                .orNull()
        }
    }

    override val routes =
        Files.list(directory).use { staticDirectoryStream ->
            staticDirectoryStream.asSequence()
                .map {
                    when {
                        it.isDirectory() -> directoryHandler(it)
                        else -> rootFileHandler(it)
                    }
                }
                .toMutableSet()
                .also { it.add(indexHandler(directory)) }
                .let { routes(*it.toTypedArray()) }
        }

}
