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
import io.javalin.plugin.openapi.annotations.ContentType.FORM_DATA_MULTIPART
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.maven.api.DeployRequest
import org.panda_lang.reposilite.web.api.Route
import org.panda_lang.reposilite.web.api.RouteMethod.POST
import org.panda_lang.reposilite.web.api.RouteMethod.PUT
import org.panda_lang.reposilite.web.api.Routes

internal class DeploymentEndpoint(private val mavenFacade: MavenFacade) : Routes {

    @OpenApi(
        path = "/:repositoryName/*",
        methods = [HttpMethod.POST, HttpMethod.PUT],
        summary = "Deploy artifact to the repository",
        description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        tags = [ "Repository" ],
        pathParams = [ OpenApiParam(name = "*", description = "Artifact path qualifier", required = true) ],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Input stream of requested file",
                content = [OpenApiContent(type = FORM_DATA_MULTIPART)]
            ),
            OpenApiResponse(status = "401", description = "Returns 401 for invalid credentials"), OpenApiResponse(
                status = "405",
                description = "Returns 405 if deployment is disabled in configuration"
            ),
            OpenApiResponse(
                status = "500",
                description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
            ),
            OpenApiResponse(
                status = "507",
                description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file"
            )
        ]
    )
    private val deployArtifact = Route("/:repositoryName/*", POST, PUT) {
        context.logger.debug("DEPLOY ${context.uri} from ${context.address}")

        authorized {
            val request = DeployRequest(parameter("repositoryName"), wildcard(), getSessionIdentifier(), context.input())

            response = mavenFacade.deployArtifact(request)
                .onError {
                    context.logger.debug("Cannot deploy artifact due to: ${it.message}")
                }
        }
    }

    override val routes = setOf(deployArtifact)

}