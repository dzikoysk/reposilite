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
package org.panda_lang.reposilite.frontend.infrastructure

import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.annotations.OpenApi
import com.dzikoysk.openapi.annotations.OpenApiResponse
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import org.panda_lang.reposilite.frontend.FrontendFacade
import org.panda_lang.reposilite.web.api.Route
import org.panda_lang.reposilite.web.api.RouteMethod.GET
import org.panda_lang.reposilite.web.api.Routes
import org.panda_lang.reposilite.web.encoding

private const val ROUTE = "/*"

internal class FrontendHandler(private val frontendFacade: FrontendFacade) : Routes {

    @OpenApi(
        path = ROUTE,
        methods = [ HttpMethod.GET ],
        summary = "Get frontend application",
        description = "Returns Vue.js application wrapped into one app.js file",
        tags = [ "Resource" ],
        responses = [ OpenApiResponse(status = "200", description = "Default response") ]
    )
    private val frontend = Route(ROUTE, GET) {
        val qualifier = wildcard(defaultValue = "index.html")

        FrontendFacade::class.java.getResourceAsStream("/static/$qualifier")
            ?.let {
                ctx.result(it)
                    .contentType(MimeTypes.getDefaultMimeByExtension(qualifier))
                    .encoding("UTF-8")
            }
            ?: ctx.status(HttpStatus.NOT_FOUND_404)
    }

    override val routes = setOf(frontend)

}