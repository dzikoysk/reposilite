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

import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.settings.SettingsFacade
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.DELETE
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.HEAD
import com.reposilite.web.routing.RouteMethod.POST
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.openapi.ContentType.FORM_DATA_MULTIPART
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class MavenEndpoints(
    private val mavenFacade: MavenFacade,
    private val frontendFacade: FrontendFacade,
    settingsFacade: SettingsFacade
) : ReposiliteRoutes() {

    private val compressionStrategy = settingsFacade.localConfiguration.compressionStrategy

    @OpenApi(
        path = "/{repository}/*",
        methods = [HttpMethod.GET],
        tags = ["Maven"],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "404", description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository")
        ]
    )
    private val findFile = ReposiliteRoute<Unit>("/{repository}/<gav>", HEAD, GET) {
        accessed {
            LookupRequest(this?.identifier, requireParameter("repository"), requireParameter("gav").toLocation()).let { request ->
                mavenFacade.findDetails(request)
                    .`is`(DocumentInfo::class.java) { ErrorResponse(NOT_FOUND, "Requested file is a directory") }
                    .flatMap { details -> mavenFacade.findFile(request).map { Pair(details, it) } }
                    .peek { (details, file) -> ctx.resultAttachment(details.name, details.contentType, details.contentLength, compressionStrategy.get(), file) }
                    .onError { ctx.status(it.status).html(frontendFacade.createNotFoundPage(uri, it.message)) }
            }
        }
    }

    @OpenApi(
        tags = [ "Maven" ],
        path = "/{repository}/*",
        methods = [HttpMethod.POST, HttpMethod.PUT],
        summary = "Deploy artifact to the repository",
        description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "401", description = "Returns 401 for invalid credentials"),
            OpenApiResponse(status = "405", description = "Returns 405 if deployment is disabled in configuration"),
            OpenApiResponse(status = "507", description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file")
        ]
    )
    private val deployFile = ReposiliteRoute<Unit>("/{repository}/<gav>", POST, PUT) {
        authorized {
            response = mavenFacade.deployFile(DeployRequest(requireParameter("repository"), requireParameter("gav").toLocation(), getSessionIdentifier(), ctx.bodyAsInputStream()))
                .onError { logger.debug("Cannot deploy artifact due to: ${it.message}") }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/{repository}/*",
        summary = "Delete the given file from repository",
        methods = [HttpMethod.DELETE],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
        ]
    )
    private val deleteFile = ReposiliteRoute<Unit>("/{repository}/<gav>", DELETE) {
        authorized {
            response = mavenFacade.deleteFile(DeleteRequest(this.identifier, requireParameter("repository"), requireParameter("gav").toLocation()))
        }
    }

    override val routes = routes(findFile, deployFile, deleteFile)

}