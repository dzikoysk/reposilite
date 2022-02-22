package com.reposilite.settings.infrastructure

import com.reposilite.settings.SharedConfigurationFacade
import com.reposilite.settings.api.SettingsResponse
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod
import io.javalin.openapi.*

internal class SharedConfigurationEndpoints(private val configurationFacade: SharedConfigurationFacade): ReposiliteRoutes() {
    @OpenApi(
        path = "/api/configuration/{name}",
        methods = [HttpMethod.GET],
        tags = ["Configuration"],
        summary = "Find configuration",
        pathParams = [OpenApiParam(name = "name", description = "Name of configuration to fetch", required = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns dto representing configuration",
                content = [OpenApiContent(from = SettingsResponse::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 if token without moderation permission has been used to access this resource"
            ),
            OpenApiResponse(status = "404", description = "Returns 404 if non-existing configuration is requested")
        ]
    )
    private val getConfiguration = ReposiliteRoute<Any>("/api/configuration/{name}", RouteMethod.GET) {
        managerOnly {
            response = configurationFacade.getConfiguration(requireParameter("name"))
        }
    }

    @OpenApi(
        path = "/api/configuration/{name}",
        methods = [HttpMethod.PUT],
        tags = ["Configuration"],
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
    private val updateConfiguration = ReposiliteRoute<Any>("/api/configuration/{name}", RouteMethod.PUT) {
        managerOnly {
            val name = requireParameter("name")
            response = configurationFacade.getClassForName(name)
                .flatMap { configurationFacade.updateConfiguration(name, ctx.bodyAsClass(it)) }
        }
    }
    override val routes = routes(getConfiguration, updateConfiguration)
}
