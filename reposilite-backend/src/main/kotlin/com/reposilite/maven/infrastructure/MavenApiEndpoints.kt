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

package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.VersionsResponse
import com.reposilite.shared.ContextDsl
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import panda.std.asSuccess

internal class MavenApiEndpoints(private val mavenFacade: MavenFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/details/{repository}/*",
        methods = [HttpMethod.GET],
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns document (different for directory and file) that describes requested resource",
                content = [OpenApiContent(from = FileDetails::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 in case of unauthorized attempt of access to private repository",
                content = [OpenApiContent(from = ErrorResponse::class)]
            ),
            OpenApiResponse(
                status = "404",
                description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
            )
        ]
    )
    private val findFileDetails: ContextDsl<FileDetails>.() -> Unit = {
        accessed {
            response = parameter("repository")
                ?.let { repository -> mavenFacade.findDetails(LookupRequest(this, repository, wildcard("gav").toLocation())) }
                ?: mavenFacade.findRepositories(this).asSuccess()
        }
    }

    private val findRepositories = ReposiliteRoute("/api/maven/details", GET, handler = findFileDetails)
    private val findRepository = ReposiliteRoute("/api/maven/details/{repository}", GET, handler = findFileDetails)
    private val findInRepository = ReposiliteRoute("/api/maven/details/{repository}/<gav>", GET, handler = findFileDetails)

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/versions/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
        ]
    )
    private val findVersions = ReposiliteRoute<VersionsResponse>("/api/maven/versions/{repository}/<gav>", GET) {
        accessed {
            response = mavenFacade.findVersions(
                VersionLookupRequest(
                    accessToken = this,
                    repository = requireParameter("repository"),
                    gav = requireParameter("gav").toLocation(),
                    filter = ctx.queryParam("filter")
                )
            )
        }
    }

    override val routes = routes(findRepositories, findRepository, findInRepository, findVersions)

}