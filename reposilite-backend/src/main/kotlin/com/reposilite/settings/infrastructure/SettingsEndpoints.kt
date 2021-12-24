package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.settings.api.SettingsUpdateRequest
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
        path = "/api/settings/content/{name}",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "Find configuration content",
        pathParams = [OpenApiParam(name = "name", description = "Name of configuration to fetch", required = true)],
        responses = [
            OpenApiResponse(status = "200", description = "Returns dto representing configuration", content = [OpenApiContent(from = SettingsResponse::class)]),
            OpenApiResponse(status = "401", description = "Returns 401 if token without moderation permission has been used to access this resource"),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration is requested")
        ]
    )
    private val findConfiguration = ReposiliteRoute("/api/settings/content/{name}", GET) {
        managerOnly {
            response = settingsFacade.resolveConfiguration(requiredParameter("name"))
        }
    }

    @OpenApi(
        path = "/api/settings/content/{name}",
        methods = [HttpMethod.PUT],
        tags = ["Settings"],
        summary = "Update configuration",
        pathParams = [OpenApiParam(name = "name", description = "Name of configuration to update", required = true)],
        responses = [
            OpenApiResponse(status = "200", description = "Returns 200 if configuration has been updated successfully"),
            OpenApiResponse(status = "401", description = "Returns 401 if token without moderation permission has been used to access this resource"),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration is requested")
        ]
    )
    private val updateConfiguration = ReposiliteRoute("/api/settings/content/{name}", PUT) {
        managerOnly {
            response = settingsFacade.updateConfiguration(SettingsUpdateRequest(requiredParameter("name"), ctx.body()))
                .map { "Success" }
        }
    }

    override val routes = setOf(findConfiguration, updateConfiguration)

}