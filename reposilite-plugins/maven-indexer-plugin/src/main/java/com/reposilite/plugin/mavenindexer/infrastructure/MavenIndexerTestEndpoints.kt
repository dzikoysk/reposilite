package com.reposilite.plugin.mavenindexer.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.infrastructure.MavenRoutes
import com.reposilite.plugin.mavenindexer.MavenIndexerFacade
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.routing.RouteMethod
import io.javalin.http.ContentType
import io.javalin.openapi.*
import panda.std.Result

internal class MavenIndexerTestEndpoints(
    private val mavenIndexerFacade: MavenIndexerFacade,
    mavenFacade: MavenFacade
) : MavenRoutes(mavenFacade) {


    @OpenApi(
        tags = ["MavenIndexer"],
        path = "/api/maven-indexer/{repository}/index",
        methods = [HttpMethod.GET], // TODO: Change
        pathParams = [
            OpenApiParam(name = "repository", description = "Repository to index", required = true),
        ],
        responses = [
            OpenApiResponse(
                "200",
                content = [OpenApiContent(from = String::class, type = ContentType.PLAIN)],
                description = ""
            ),
        ]
    )
    private val index = ReposiliteRoute<Any>("/api/maven-indexer/{repository}/index", RouteMethod.GET) {
//        authorized { // TODO
        requireRepository { repository ->
            mavenIndexerFacade.indexRepository(repository)
            response = Result.ok("ok")
        }
//        }
    }

    override val routes = routes(index)
}