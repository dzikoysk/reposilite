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
import io.javalin.http.Context
import org.panda_lang.reposilite.frontend.FrontendFacade
import org.panda_lang.reposilite.web.api.RouteHandler
import org.panda_lang.reposilite.web.api.RouteMethod.GET

internal class FrontendHandler(private val frontendFacade: FrontendFacade) : RouteHandler {

    override val route = "/js/app.js"
    override val methods = listOf(GET)

    @OpenApi(
        path = "/js/app.js",
        method = HttpMethod.GET,
        operationId = "getApp",
        summary = "Get frontend application",
        description = "Returns Vue.js application wrapped into one app.js file",
        tags = [ "Resource" ],
        responses = [ OpenApiResponse(status = "200", description = "Default response") ]
    )
    override fun handle(context: Context) {
        with(context.result(frontendFacade.getApp())) {
            header("Content-Type", "application/javascript")
            res.characterEncoding = "UTF-8"
        }
    }

}