package com.reposilite.settings.infrastructure

import com.reposilite.settings.SettingsFacade
import com.reposilite.settings.api.SettingsUpdateRequest
import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi

internal class SettingsEndpoints(private val settingsFacade: SettingsFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/settings/content/{name}",
        methods = [HttpMethod.GET],
        tags = ["Settings"],
        summary = "Find configuration content"
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
        summary = "Update configuration"
    )
    private val updateConfiguration = ReposiliteRoute("/api/settings/content/{name}", PUT) {
        managerOnly {
            response = settingsFacade.updateConfiguration(SettingsUpdateRequest(requiredParameter("name"), ctx.body()))
                .map { "Success" }
        }
    }

    override val routes = setOf(findConfiguration, updateConfiguration)

}