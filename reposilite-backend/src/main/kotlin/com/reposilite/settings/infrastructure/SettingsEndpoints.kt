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

package com.reposilite.settings.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.reposilite.settings.SettingsFacade
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class SettingsEndpoints(private val settingsFacade: SettingsFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/configuration/{name}",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "Find configuration",
        pathParams = [OpenApiParam(name = "name", description = "Name of configuration to fetch", required = true)],
        responses = [
            OpenApiResponse(status = "200", description = "Returns dto representing configuration"),
            OpenApiResponse(status = "401", description = "Returns 401 if token without moderation permission has been used to access this resource"),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration is requested")
        ]
    )
    private val getConfiguration = ReposiliteRoute<Any>("/api/configuration/{name}", GET) {
        managerOnly {
            response = settingsFacade.getSettings(requireParameter("name"))
        }
    }

    @OpenApi(
        path = "/api/configuration/{name}",
        methods = [HttpMethod.PUT],
        tags = ["Settings"],
        summary = "Update configuration",
        pathParams = [OpenApiParam(name = "name", description = "Name of configuration to update", required = true)],
        responses = [
            OpenApiResponse(status = "200", description = "Returns 200 if configuration has been updated successfully"),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 if token without moderation permission has been used to access this resource"
            ),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration is requested")
        ]
    )
    private val updateConfiguration = ReposiliteRoute<Any>("/api/configuration/{name}", PUT) {
        managerOnly {
            with(requireParameter("name")) {
                response = settingsFacade
                    .getSettingsClass(this)
                    .flatMap { settingsFacade.updateSettings(this, ctx.bodyAsClass(it)) }
            }
        }
    }

    @OpenApi(
        path = "/api/schema/{name}",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "Get schema",
        pathParams = [OpenApiParam(name = "name", description = "Name of schema to get", required = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns dto representing configuration schema",
                content = [OpenApiContent(from = JsonNode::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 if token without moderation permission has been used to access this resource"
            ),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration schema is requested")
        ]
    )
    private val getSchema = ReposiliteRoute<Any>("/api/schema/{name}", GET) {
        managerOnly {
            response = settingsFacade.getSchema(requireParameter("name"))
        }
    }

    override val routes = routes(getConfiguration, updateConfiguration, getSchema)

}
