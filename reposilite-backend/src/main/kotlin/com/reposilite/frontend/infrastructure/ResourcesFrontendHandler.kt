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
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.Context
import panda.std.Result

internal class ResourcesFrontendHandler(frontendFacade: FrontendFacade) : FrontendHandler(frontendFacade) {

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
        respondWithFile(ctx, uri, FrontendFacade::class.java.getResourceAsStream("/static/$uri"))

    override val routes = setOf(defaultHandler, indexHandler, assetsHandler)

}