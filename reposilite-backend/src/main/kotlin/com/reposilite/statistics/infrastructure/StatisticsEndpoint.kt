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

package com.reposilite.statistics.infrastructure

import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.statistics.MAX_PAGE_SIZE
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.api.AllResolvedResponse
import com.reposilite.statistics.api.ResolvedCountResponse
import com.reposilite.statistics.api.ResolvedEntriesResponse
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import io.javalin.community.routing.Route.GET
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
        val repository = requireParameter("repository")
        val gav = requireParameter("gav")
        authorized(resolvedPath(repository, gav)) {
            val limit = requireParameter("limit").toIntOrNull()
            response = limit
                ?.let { statisticsFacade.findResolvedRequestsByPhrase(repository, gav, it, identifier) }
                ?: badRequestError("Requested invalid page size (${requireParameter("limit")}, expected 1..$MAX_PAGE_SIZE)")
        }
    }

    @OpenApi(
        tags = ["Statistics"],
        path = "/api/statistics/resolved/entries",
        methods = [HttpMethod.GET],
        queryParams = [
            OpenApiParam(name = "limit", description = "Amount of entries to find (Maximum: $MAX_PAGE_SIZE)", required = false),
            OpenApiParam(name = "offset", description = "Amount of entries to skip", required = false),
            OpenApiParam(name = "repository", description = "Repository to search in. If omitted, all repositories are searched.", required = false),
            OpenApiParam(name = "phrase", description = "Phrase to search for", required = false)
        ],
        responses = [
            OpenApiResponse("200", content = [ OpenApiContent(from = ResolvedEntriesResponse::class) ], description = "Paginated resolved entry statistics"),
            OpenApiResponse("401", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When non-manager token is used")
        ]
    )
    val findEntries = ReposiliteRoute<ResolvedEntriesResponse>("/api/statistics/resolved/entries", GET) {
        managerOnly {
            response = statisticsFacade.findResolvedEntries(
                repository = ctx.queryParam("repository")?.takeIf(String::isNotBlank),
                phrase = ctx.queryParam("phrase").orEmpty(),
                limit = ctx.queryParam("limit")?.toIntOrNull() ?: MAX_PAGE_SIZE,
                offset = ctx.queryParam("offset")?.toLongOrNull() ?: 0
            )
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

    @OpenApi(
        tags = ["Statistics"],
        path = "/api/statistics/resolved/all",
        methods = [HttpMethod.GET],
        responses = [
            OpenApiResponse("200", content = [ OpenApiContent(from = AllResolvedResponse::class) ], description = "Aggregated list of statistics per each repository"),
            OpenApiResponse("401", content = [ OpenApiContent(from = ErrorResponse::class) ], description = "When non-manager token is used")
        ]
    )
    val getAllStatistics = ReposiliteRoute<AllResolvedResponse>("/api/statistics/resolved/all", GET) {
        managerOnly {
            response = statisticsFacade.getAllResolvedStatistics()
        }
    }

    override val routes = routes(findCountByPhrase, findEntries, findUniqueCount, getAllStatistics)

    private fun resolvedPath(repository: String, gav: String): String =
        "/$repository/${gav.trimStart('/')}"

}
