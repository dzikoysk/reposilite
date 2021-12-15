package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.VersionResponse
import com.reposilite.shared.fs.DocumentInfo
import com.reposilite.shared.fs.FileDetails
import com.reposilite.token.api.AccessToken
import com.reposilite.web.ContextDsl
import com.reposilite.web.application.ReposiliteRoute
import com.reposilite.web.application.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.ContentType
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import panda.std.Result
import panda.std.asError
import panda.std.asSuccess

internal class MavenLatestApiEndpoints(private val mavenFacade: MavenFacade) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/version/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
            OpenApiParam(name = "type", description = "Format of expected response type: empty (default) for json; 'raw' for plain text", required = false),
        ],
        responses = [
            OpenApiResponse("200", content = [OpenApiContent(from = VersionResponse::class)], description = "default response"),
            OpenApiResponse("200", content = [OpenApiContent(from = String::class, type = ContentType.PLAIN)], description = ""),
        ]
    )
    private val findLatestVersion = ReposiliteRoute("/api/maven/latest/version/{repository}/<gav>", GET) {
        accessed {
            response = VersionLookupRequest(this, requiredParameter("repository"), requiredParameter("gav"), ctx.queryParam("filter"))
                .let { mavenFacade.findLatest(it) }
                .flatMap {
                    when (val type = ctx.queryParam("type")) {
                        "raw" -> it.version.asSuccess()
                        "json", null -> it.asSuccess()
                        else -> ErrorResponse(BAD_REQUEST, "Unsupported response type: $type").asError()
                    }
                }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/details/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [ OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false) ],
        responses = [ OpenApiResponse("200", content = [OpenApiContent(from = FileDetails::class)]) ]
    )
    private val findLatestDetails = ReposiliteRoute("/api/maven/latest/details/{repository}/<gav>", GET) {
        accessed {
            response = resolveLatestArtifact(this@ReposiliteRoute, this) {
                mavenFacade.findDetails(it)
            }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/file/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [ OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false) ]
    )
    private val findLatestFile = ReposiliteRoute("/api/maven/latest/file/{repository}/<gav>", GET) {
        accessed {
            response = resolveLatestArtifact(this@ReposiliteRoute, this) { lookupRequest ->
                mavenFacade.findDetails(lookupRequest)
                    .`is`(DocumentInfo::class.java) { ErrorResponse(BAD_REQUEST, "Requested file is a directory") }
                    .peek { ctx.contentType(it.contentType) }
                    .flatMap { mavenFacade.findFile(lookupRequest) }
            }
        }
    }

    private fun <T> resolveLatestArtifact(context: ContextDsl, accessToken: AccessToken?, request: (LookupRequest) -> Result<T, ErrorResponse>): Result<T, ErrorResponse> {
        val repository = context.requiredParameter("repository")
        val gav = context.requiredParameter("gav")

        return VersionLookupRequest(accessToken, repository, gav, context.ctx.queryParam("filter"))
            .let { mavenFacade.findLatest(it) }
            .map { LookupRequest(accessToken, repository, "$gav/${it.version}/${gav.substringAfterLast("/", gav)}-${it.version}.jar") }
            .flatMap { request(it) }
    }

    override val routes = setOf(findLatestVersion, findLatestDetails, findLatestFile)

}