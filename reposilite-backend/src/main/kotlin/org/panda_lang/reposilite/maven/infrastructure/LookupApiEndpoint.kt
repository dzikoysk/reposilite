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
package org.panda_lang.reposilite.maven.infrastructure

import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.annotations.OpenApi
import com.dzikoysk.openapi.annotations.OpenApiContent
import com.dzikoysk.openapi.annotations.OpenApiParam
import com.dzikoysk.openapi.annotations.OpenApiResponse
import io.javalin.http.Context
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.maven.api.FileDetails
import org.panda_lang.reposilite.maven.api.FileListResponse
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.panda_lang.reposilite.web.api.RouteHandler
import org.panda_lang.reposilite.web.api.RouteMethod.GET
import org.panda_lang.reposilite.web.api.RouteMethod.HEAD
import org.panda_lang.reposilite.web.context

private const val ROUTE = "/api/:repositoryName/*"

internal class IndexEndpoint(
    private val contextFactory: ReposiliteContextFactory,
    private val mavenFacade: MavenFacade
) : RouteHandler {

    override val route = ROUTE
    override val methods = listOf(HEAD, GET)

    @OpenApi(
        path = ROUTE,
        methods = [HttpMethod.HEAD, HttpMethod.GET],
        operationId = "repositoryApi",
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        tags = ["Repository"],
        pathParams = [OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)],
        responses = [OpenApiResponse(
            status = "200",
            description = "Returns document (different for directory and file) that describes requested resource",
            content = [OpenApiContent(from = FileDetails::class), OpenApiContent(from = FileListResponse::class)]
        ), OpenApiResponse(
            status = "401",
            description = "Returns 401 in case of unauthorized attempt of access to private repository",
            content = [OpenApiContent(from = ErrorResponse::class)]
        ), OpenApiResponse(
            status = "404",
            description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
        )]
    )
    override fun handle(ctx: Context) = context(contextFactory, ctx) {
        context.logger.info("Lookup API ${context.uri} from ${context.address}")

        accessed {
            val request = LookupRequest(parameter("repositoryName"), wildcard(), this?.getSessionIdentifier() ?: context.address, this?.accessToken)
            response = mavenFacade.lookup(request).map { it.fileDetails }
        }
    }

}