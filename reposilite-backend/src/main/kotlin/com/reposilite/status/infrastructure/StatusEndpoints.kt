/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.status.infrastructure

import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
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