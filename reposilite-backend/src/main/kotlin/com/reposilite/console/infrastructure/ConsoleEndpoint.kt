/*
 * Copyright (c) 2022 dzikoysk
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
package com.reposilite.console.infrastructure

import com.reposilite.console.ConsoleFacade
import com.reposilite.console.MAX_COMMAND_LENGTH
import com.reposilite.console.api.ExecutionResponse
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.unauthorizedError
import com.reposilite.web.routing.RouteMethod.POST
import io.javalin.openapi.*

internal class ConsoleEndpoint(private val consoleFacade: ConsoleFacade) : ReposiliteRoutes() {

    @OpenApi(
        path = "/api/console/execute",
        methods = [HttpMethod.POST],
        summary = "Remote command execution",
        description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
        tags = ["Cli"],
        headers = [OpenApiParam(name = "Authorization", description = "Name and secret provided as basic auth credentials", required = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Status of the executed command",
                content = [OpenApiContent(from = ExecutionResponse::class)]
            ),
            OpenApiResponse(
                status = "400",
                description = "Error message related to the invalid command format (0 < command length < $MAX_COMMAND_LENGTH)",
                content = [OpenApiContent(from = ErrorResponse::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Error message related to the unauthorized access",
                content = [OpenApiContent(from = ErrorResponse::class)]
            )
        ]
    )
    private val executeCommand = ReposiliteRoute<ExecutionResponse>("/api/console/execute", POST) {
        logger.info("REMOTE EXECUTION $uri from ${ctx.ip()}")

        authenticated {
            if (!isManager()) {
                response = unauthorizedError("Authenticated user is not a manager")
                return@authenticated
            }

            logger.info("$name (${ctx.ip()}) requested command: ${ctx.body()}")
            response = consoleFacade.executeCommand(ctx.body())
        }
    }

    override val routes = routes(executeCommand)

}
