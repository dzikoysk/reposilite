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

package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.extensions.encoding
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.toLocation
import com.reposilite.storage.getExtension
import com.reposilite.storage.getSimpleName
import com.reposilite.storage.inputStream
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.ContentType
import io.javalin.http.Context
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence

internal sealed class FrontendHandler(private val frontendFacade: FrontendFacade) : ReposiliteRoutes() {

    protected fun respondWithFile(ctx: Context, uri: String, source: () -> InputStream?): Result<InputStream, ErrorResponse> {
        val contentType = ContentType.getContentTypeByExtension(uri.getExtension())
        ctx.contentType(contentType?.mimeType ?: ContentType.OCTET_STREAM)

        return when (contentType?.mimeType?.let { it.startsWith("text") || it.startsWith("application") }) {
            true -> respondWithProcessedFile(ctx, uri, source)
            else -> respondWithRawFile(source)
        }
    }

    private fun respondWithProcessedFile(ctx: Context, uri: String, source: () -> InputStream?): Result<InputStream, ErrorResponse> =
        frontendFacade.resolve(uri) { source()?.readAllBytes()?.decodeToString() }
            ?.let {
                ctx.encoding(UTF_8)
                ok(it.toByteArray().inputStream())
            }
            ?: notFoundError("Resource not found")

    private fun respondWithRawFile(source: () -> InputStream?): Result<InputStream, ErrorResponse> =
        source()
            ?.asSuccess()
            ?: notFoundError("Resource not found")

}

internal class ResourcesFrontendHandler(frontendFacade: FrontendFacade, private val resourcesDirectory: String) : FrontendHandler(frontendFacade) {

    private val defaultHandler = ReposiliteRoute<InputStream>("/", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val indexHandler = ReposiliteRoute<InputStream>("/index.html", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val assetsHandler = ReposiliteRoute<InputStream>("/assets/<path>", GET) {
        response = respondWithResource(ctx, "assets/${ctx.pathParam("path")}")
    }

    private fun respondWithResource(ctx: Context, uri: String): Result<InputStream, ErrorResponse> =
        respondWithFile(ctx, uri) {
            FrontendFacade::class.java.getResourceAsStream("/$resourcesDirectory/$uri") ?: "".toByteArray().inputStream()
        }

    override val routes = routes(defaultHandler, indexHandler, assetsHandler)

}

internal class CustomFrontendHandler(frontendFacade: FrontendFacade, directory: Path) : FrontendHandler(frontendFacade) {

    override val routes =
        Files.list(directory).use { staticDirectoryStream ->
            staticDirectoryStream.asSequence()
                .map {
                    if (it.isDirectory())
                        ReposiliteRoute<InputStream>("/${it.fileName}/<path>", GET) {
                            response = respondWithFile(ctx, it.getSimpleName()) {
                                parameter("path")
                                    .toLocation()
                                    .toPath()
                                    .map { path -> it.resolve(path) }
                                    .flatMap { path -> path.inputStream().mapErr { error -> error.message } }
                                    .orNull()
                            }
                        }
                    else
                        ReposiliteRoute("/${it.getSimpleName()}", GET) {
                            response = respondWithFile(ctx, it.getSimpleName()) {
                                it.inputStream().orNull()
                            }
                        }
                }
                .toMutableSet()
                .also {
                    it.add(ReposiliteRoute("/", GET) {
                        response = respondWithFile(ctx, "index.html") {
                            directory.resolve("index.html")
                                .inputStream()
                                .orNull()
                        }
                    })
                }
                .let { routes(*it.toTypedArray()) }
        }

}
