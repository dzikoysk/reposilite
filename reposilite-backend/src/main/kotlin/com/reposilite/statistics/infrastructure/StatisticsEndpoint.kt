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

package com.reposilite.statistics.infrastructure

import com.reposilite.statistics.MAX_PAGE_SIZE
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam

internal class StatisticsEndpoint(private val statisticsFacade: StatisticsFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/statistics/resolved/{limit}/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "limit", description = "Amount of records to find (Maximum: $MAX_PAGE_SIZE", required = true),
            OpenApiParam(name = "repository", description = "Repository to search in", required = true),
            OpenApiParam(name = "*", description = "Phrase to search for", required = true, allowEmptyValue = true)
        ],
    )
    val findCount = ReposiliteRoute("/api/statistics/resolved/{limit}/{repository}/<gav>", GET) {
        authorized("/${requiredParameter("repository")}/${requiredParameter("gav")}") {
            response = statisticsFacade.findResolvedRequestsByPhrase(requiredParameter("repository"), requiredParameter("gav"), 1)
        }
    }

    override val routes = setOf(findCount)

}