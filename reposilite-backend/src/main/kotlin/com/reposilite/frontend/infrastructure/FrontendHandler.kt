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

import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import com.reposilite.frontend.FrontendFacade
import com.reposilite.web.api.Route
import com.reposilite.web.api.RouteMethod.GET
import com.reposilite.web.api.Routes
import com.reposilite.web.encoding

internal class FrontendHandler(private val frontendFacade: FrontendFacade) : Routes {

    private val defaultHandler = Route("/", GET) {
        bindResource(ctx, "index.html")
    }

    private val indexHandler = Route("/index.html", GET) {
        bindResource(ctx, "index.html")
    }

    private val assetsHandler = Route("/assets/*", GET) {
        bindResource(ctx, "assets/${wildcard()}")
    }

    override val routes = setOf(defaultHandler, indexHandler, assetsHandler)

    private fun bindResource(ctx: Context, uri: String) {
        FrontendFacade::class.java.getResourceAsStream("/static/$uri")
            ?.let {
                ctx.result(it)
                    .encoding(Charsets.UTF_8)
                    .contentType(MimeTypes.getDefaultMimeByExtension(uri))
            }
            ?: ctx.status(HttpStatus.NOT_FOUND_404)
    }

}