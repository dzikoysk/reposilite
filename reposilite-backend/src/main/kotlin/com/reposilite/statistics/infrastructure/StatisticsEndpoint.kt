package com.reposilite.statistics.infrastructure

import com.reposilite.statistics.StatisticsFacade
import com.reposilite.web.ReposiliteRoute
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam

internal class StatisticsEndpoint(private val statisticsFacade: StatisticsFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/statistics/count/{type}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "type", description = "Record type", required = true),
            OpenApiParam(name = "*", description = "Record identifier", required = true, allowEmptyValue = true)
        ],
    )
    val findCount = ReposiliteRoute("/api/statistics/count/{type}/<identifier>", GET) {
        response = statisticsFacade.findRecordsByPhrase(parameter("type"), parameter("identifier"))
            .map { records -> records.sumOf { it.count } }
    }

    override val routes = setOf(findCount)

}