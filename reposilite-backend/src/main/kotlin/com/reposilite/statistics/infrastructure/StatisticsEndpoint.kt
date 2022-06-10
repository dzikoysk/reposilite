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

package com.reposilite.statistics.infrastructure

import com.reposilite.statistics.MAX_PAGE_SIZE
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.ResolvedCountResponse
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

internal class StatisticsEndpoint(private val statisticsFacade: StatisticsFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Statistics"],
        path = "/api/statistics/resolved/phrase/{limit}/{repository}/{gav}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "limit", description = "Amount of records to find (Maximum: $MAX_PAGE_SIZE", required = true),
            OpenApiParam(name = "repository", description = "Repository to search in", required = true),
            OpenApiParam(name = "gav", description = "Phrase to search for", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse("200", content = [ OpenApiContent(from = ResolvedCountResponse::class) ], description = "Aggregated sum of resolved requests with list a list of them all"),
            OpenApiResponse("401", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When invalid token is used")
        ]
    )
    val findCountByPhrase = ReposiliteRoute<ResolvedCountResponse>("/api/statistics/resolved/phrase/{limit}/{repository}/<gav>", GET) {
        authorized("/${requireParameter("repository")}/${requireParameter("gav")}") {
            response = statisticsFacade.findResolvedRequestsByPhrase(requireParameter("repository"), requireParameter("gav"), 1)
        }
    }

    @OpenApi(
        tags = ["Statistics"],
        path = "/api/statistics/resolved/unique",
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", content = [ OpenApiContent(from = Long::class) ], description = "Number of all unique requests"),
            OpenApiResponse("401", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When non-manager token is used")
        ]
    )
    val findUniqueCount = ReposiliteRoute<Long>("/api/statistics/resolved/unique", GET) {
        managerOnly {
            response = statisticsFacade.countUniqueRecords().asSuccess()
        }
    }

    override val routes = routes(findCountByPhrase, findUniqueCount)

}
