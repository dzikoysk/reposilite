package com.reposilite.status.infrastructure

import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import panda.std.asSuccess

internal class StatusEndpoints(private val statusFacade: StatusFacade, val failureFacade: FailureFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/status/instance",
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = InstanceStatusResponse::class)])
        ]
    )
    private val getInstanceStatus = ReposiliteRoute<InstanceStatusResponse>("/api/status/instance", GET) {
        managerOnly {
            response = statusFacade.fetchInstanceStatus().asSuccess()
        }
    }

    @OpenApi(
        path = "/api/status/snapshots",
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = Array<StatusSnapshot>::class)])
        ]
    )
    private val getStatusSnapshots = ReposiliteRoute<Array<StatusSnapshot>>("/api/status/snapshots", GET) {
        managerOnly {
            response = statusFacade.getLatestStatusSnapshots().asSuccess()
        }
    }

    override val routes = routes(getInstanceStatus, getStatusSnapshots)

}