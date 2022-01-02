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

package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.storage.getExtension
import com.reposilite.storage.getSimpleName
import com.reposilite.storage.inputStream
import com.reposilite.storage.toLocation
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.encoding
import com.reposilite.web.http.errorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.ContentType
import io.javalin.http.ContentType.Companion.PLAIN
import io.javalin.http.Context
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import panda.std.Result.ok
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.isDirectory
import kotlin.text.Charsets.UTF_8

internal sealed class FrontendHandler(private val frontendFacade: FrontendFacade) : ReposiliteRoutes() {

    protected fun respondWithFile(ctx: Context, uri: String, source: () -> String?): Result<String, ErrorResponse> =
        frontendFacade.resolve(uri, source)
            ?.let {
                ctx.encoding(UTF_8)
                ctx.contentType(ContentType.getMimeTypeByExtension(uri.getExtension()) ?: PLAIN)
                ok(it)
            }
            ?: errorResponse(NOT_FOUND, "Resource not found");

}

internal class ResourcesFrontendHandler(frontendFacade: FrontendFacade, val resourcesDirectory: String) : FrontendHandler(frontendFacade) {

    private val defaultHandler = ReposiliteRoute("/", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val indexHandler = ReposiliteRoute("/index.html", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val assetsHandler = ReposiliteRoute("/assets/<path>", GET) {
        response = respondWithResource(ctx, "assets/${ctx.pathParam("path")}")
    }

    private fun respondWithResource(ctx: Context, uri: String): Result<String, ErrorResponse> =
        respondWithFile(ctx, uri) {
            FrontendFacade::class.java.getResourceAsStream("/$resourcesDirectory/$uri")
                ?.use { it.readBytes().decodeToString() }
                ?: ""
        }

    override val routes =
        setOf(defaultHandler, indexHandler, assetsHandler)

}

internal class CustomFrontendHandler(frontendFacade: FrontendFacade, directory: Path) : FrontendHandler(frontendFacade) {

    override val routes: Set<ReposiliteRoute> = run {
        val routes = Files.list(directory)
            .map {
                if (it.isDirectory()) ReposiliteRoute("/${it.fileName}/<path>", GET) {
                    response = respondWithFile(ctx, it.getSimpleName()) { it.resolve(ctx.pathParam("path").toLocation().toPath().orElseGet { Paths.get("") }).decodeToString().orNull() }
                }
                else ReposiliteRoute("/${it.getSimpleName()}", GET) {
                    response = respondWithFile(ctx, it.getSimpleName()) { it.decodeToString().orNull() }
                }
            }
            .collect(Collectors.toSet())

        routes.add(ReposiliteRoute("/", GET) {
            response = respondWithFile(ctx, "index.html") { directory.resolve("index.html").decodeToString().orNull() }
        })

        routes
    }

    private fun Path.decodeToString(): Result<String, ErrorResponse> =
        inputStream()
            .map { it.use { input -> input.readBytes().decodeToString() } }

}