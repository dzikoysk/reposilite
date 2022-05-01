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

import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.Settings
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.errorResponse
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class SettingsEndpoints(private val settingsFacade: SettingsFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/settings/domains",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "List configurations",
        responses = [
            OpenApiResponse(status = "200", description = "Returns list of configuration names"),
            OpenApiResponse(status = "401", description = "Returns 401 if token without moderation permission has been used to access this resource")
        ]
    )
    private val listConfigurations = ReposiliteRoute<Collection<String>>("/api/settings/domains", GET) {
        managerOnly {
            response = settingsFacade.names()
        }
    }

    @OpenApi(
        path = "/api/settings/schema/{name}",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "Get schema",
        pathParams = [OpenApiParam(name = "name", description = "Name of schema to get", required = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns dto representing configuration schema",
                content = [OpenApiContent(from = String::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 if token without moderation permission has been used to access this resource"
            ),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration schema is requested")
        ]
    )
    private val getSchema = ReposiliteRoute<String>("/api/settings/schema/{name}", GET) {
        managerOnly {
            response = settingsFacade.getSchema(requireParameter("name"))
                .map { it.toPrettyString() }
        }
    }

    @OpenApi(
        path = "/api/settings/domain/{name}",
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
    private val getConfiguration = ReposiliteRoute<Settings>("/api/settings/domain/{name}", GET) {
        managerOnly {
            response = settingsFacade.getSettings(requireParameter("name"))
        }
    }

    @OpenApi(
        path = "/api/settings/domain/{name}",
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
    private val updateConfiguration = ReposiliteRoute<Settings>("/api/settings/domain/{name}", PUT) {
        managerOnly {
            with(requireParameter("name")) {
                response = ctx.body()
                    .takeIf { it.isNotEmpty() }
                    ?.let { settingsFacade.getSettingsClass(this) }
                    ?.flatMap { settingsFacade.updateSettings(this, ctx.bodyAsClass(it)) }
                    ?: errorResponse(BAD_REQUEST, "Body is empty")
            }
        }
    }

    override val routes = routes(listConfigurations, getConfiguration, updateConfiguration, getSchema)

}
