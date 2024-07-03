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

package com.reposilite.maven.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.shared.extensions.uri
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.web.api.ReposiliteRoute
import io.javalin.community.routing.Route.DELETE
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.HEAD
import io.javalin.community.routing.Route.POST
import io.javalin.community.routing.Route.PUT
import io.javalin.http.Context
import io.javalin.openapi.ContentType.FORM_DATA_MULTIPART
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

const val X_GENERATE_CHECKSUMS = "X-Generate-Checksums"

internal class MavenEndpoints(
    mavenFacade: MavenFacade,
    private val frontendFacade: FrontendFacade,
    private val compressionStrategy: String
) : MavenRoutes(mavenFacade) {

    @OpenApi(
        path = "/{repository}/{gav}",
        methods = [HttpMethod.GET],
        tags = ["Maven"],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "404", description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository")
        ]
    )
    private val findFile = ReposiliteRoute<Unit>("/{repository}/<gav>", HEAD, GET) {
        accessed {
            requireGav { gav ->
                findFile(ctx, this?.identifier, requireParameter("repository"), gav)
            }
        }
    }

    fun findFile(ctx: Context, identifier: AccessTokenIdentifier?, repository: String, gav: Location) {
        LookupRequest(identifier, repository, gav)
            .let { request -> mavenFacade.findFile(request) }
            .peek {
                ctx.resultAttachment(
                    name = it.document.name,
                    contentType = it.document.contentType,
                    contentLength = it.document.contentLength,
                    lastTimeModified = it.document.lastModifiedTime,
                    compressionStrategy = compressionStrategy,
                    cache = it.cachable,
                    data = it.content
                )
            }
            .onError {
                ctx.status(it.status).html(frontendFacade.createNotFoundPage(ctx.uri(), it.message))
                mavenFacade.logger.debug("FIND | Could not find file due to $it")
            }
    }

    @OpenApi(
        tags = [ "Maven" ],
        path = "/{repository}/{gav}",
        methods = [HttpMethod.POST, HttpMethod.PUT],
        summary = "Deploy artifact to the repository",
        description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true)
        ],
        headers = [
            OpenApiParam(name = X_GENERATE_CHECKSUMS, description = "Determines if Reposilite should generate checksums for this file", required = false)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "401", description = "Returns 401 for invalid credentials"),
            OpenApiResponse(status = "507", description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file")
        ]
    )
    private val deployFile = ReposiliteRoute<Unit>("/{repository}/<gav>", POST, PUT) {
        authorized {
            requireGav { gav ->
                requireRepository { repository ->
                    response = DeployRequest(repository, gav, getSessionIdentifier(), ctx.bodyInputStream(), ctx.header(X_GENERATE_CHECKSUMS) == "true")
                        .let { request -> mavenFacade.deployFile(request) }
                        .onError { error -> logger.debug("Cannot deploy artifact due to: ${error.message}") }
                }
            }

        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/{repository}/{gav}",
        summary = "Delete the given file from repository",
        methods = [HttpMethod.DELETE],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true)
        ]
    )
    private val deleteFile = ReposiliteRoute<Unit>("/{repository}/<gav>", DELETE) {
        authorized {
            requireGav { gav ->
                requireRepository { repository ->
                    response = mavenFacade.deleteFile(DeleteRequest(this.identifier, repository, gav, getSessionIdentifier()))
                }
            }
        }
    }

    override val routes = routes(findFile, deployFile, deleteFile)

}
