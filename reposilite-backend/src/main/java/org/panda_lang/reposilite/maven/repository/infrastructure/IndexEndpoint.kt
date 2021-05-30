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
package org.panda_lang.reposilite.maven.repository.infrastructure

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.repository.RepositoryService
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.maven.repository.api.FileListResponse
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.panda_lang.reposilite.web.context

internal class IndexEndpoint(
    private val contextFactory: ReposiliteContextFactory,
    private val repositoryService: RepositoryService
) : Handler {

    @OpenApi(
        operationId = "repositoryApi",
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        tags = ["Repository"],
        pathParams = [OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)],
        responses = [OpenApiResponse(
            status = "200",
            description = "Returns document (different for directory and file) that describes requested resource",
            content = [OpenApiContent(from = FileDetailsResponse::class), OpenApiContent(from = FileListResponse::class)]
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
        context.logger.info("API ${context.uri} from ${context.address}")

        authenticated {
            // auth paths
        }

        /*

        should be more like a

        hasAccess { // session based for private routes and anonymous for public

        }

         */
    }

}