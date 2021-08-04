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
import com.reposilite.web.api.Route
import com.reposilite.web.api.RouteMethod.GET
import io.javalin.http.Context

internal class ResourcesFrontendHandler(frontendFacade: FrontendFacade) : FrontendHandler(frontendFacade) {

    private val defaultHandler = Route("/", GET) {
        respondWithResource(ctx, "index.html")
    }

    private val indexHandler = Route("/index.html", GET) {
        respondWithResource(ctx, "index.html")
    }

    private val assetsHandler = Route("/assets/*", GET) {
        respondWithResource(ctx, "assets/${wildcard()}")
    }

    private fun respondWithResource(ctx: Context, uri: String) {
        respondWithFile(ctx, uri, FrontendFacade::class.java.getResourceAsStream("/static/$uri"))
    }

    override val routes = setOf(defaultHandler, indexHandler, assetsHandler)

}