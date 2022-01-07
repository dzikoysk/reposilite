package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.settings.SettingsFacade
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.extensions.letIf
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.storage.Location
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.toLocation
import com.reposilite.token.api.AccessToken
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.contentDisposition
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

private typealias RequestFunction<T> = (LookupRequest) -> Result<T, ErrorResponse>

internal class MavenLatestApiEndpoints(
    private val mavenFacade: MavenFacade,
    settingsFacade: SettingsFacade
) : ReposiliteRoutes() {

    private val compressionStrategy = settingsFacade.localConfiguration.compressionStrategy

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
            OpenApiResponse("200", content = [OpenApiContent(from = LatestVersionResponse::class)], description = "default response"),
            OpenApiResponse("200", content = [OpenApiContent(from = String::class, type = ContentType.PLAIN)], description = ""),
        ]
    )
    private val findLatestVersion = ReposiliteRoute("/api/maven/latest/version/{repository}/<gav>", GET) {
        accessed {
            response = VersionLookupRequest(this, requireParameter("repository"), requireParameter("gav").toLocation(), ctx.queryParam("filter"))
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
        responses = [ OpenApiResponse("200", description = "Details about the given file", content = [OpenApiContent(from = FileDetails::class)]) ]
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
                    .flatMap { mavenFacade.findFile(lookupRequest).map { data -> Pair(it, data) } }
                    .map { (details, file) -> ctx.resultAttachment(details.name, details.contentType, details.contentLength, compressionStrategy.get(), file) }
            }
        }
    }

    private fun <T> resolveLatestArtifact(context: ContextDsl, accessToken: AccessToken?, request: RequestFunction<T>): Result<T, ErrorResponse> =
        resolveLatestArtifact(context.requireParameter("repository"), context.requireParameter("gav").toLocation(), context.ctx.queryParam("filter"), accessToken, request)

    private fun <T> resolveLatestArtifact(repository: String, gav: Location, filter: String?, accessToken: AccessToken?, request: RequestFunction<T>): Result<T, ErrorResponse> =
        VersionLookupRequest(accessToken, repository, gav, filter)
            .let { mavenFacade.findLatest(it) }
            .flatMap { (isSnapshot, version) ->
                LookupRequest(
                    accessToken,
                    repository,
                    if (isSnapshot)
                        "$gav/${gav.locationBeforeLast("/", "").locationAfterLast("/", "")}-$version.jar".toLocation()
                    else
                        "$gav/$version/${gav.locationAfterLast("/", "")}-$version.jar".toLocation()
                )
                .let { request(it) }
                .letIf({ it.isErr && version.contains("-SNAPSHOT", ignoreCase = true) }) {
                    resolveLatestArtifact(repository, "$gav/$version".toLocation(), filter, accessToken, request)
                }
            }

    @OpenApi(
        path = "/api/badge/latest/{repository}/{gav}", // Rename 'badge/latest' to 'maven/latest/badge'?
        tags = ["badge"],
        pathParams = [
            OpenApiParam(name = "repository", description = "Artifact's repository", required = true),
            OpenApiParam(name = "gav", description = "Artifacts' GAV", required = true)
        ],
        methods = [HttpMethod.GET]
    )
    val latestBadge = ReposiliteRoute("/api/badge/latest/{repository}/<gav>", GET) {
        response = mavenFacade.findLatestBadge(
                LatestBadgeRequest(
                    repository = requireParameter("repository"),
                    gav = requireParameter("gav").toLocation(),
                    name = ctx.queryParam("name"),
                    color = ctx.queryParam("color"),
                    prefix = ctx.queryParam("prefix"),
                    filter = ctx.queryParam("filter")
                )
            )
            .peek { ctx.run {
                contentType("image/svg+xml")
                header("pragma", "no-cache")
                header("expires", "0")
                header("cache-control", "no-cache, no-store, must-revalidate, max-age=0")
                contentDisposition("inline; filename=\"latest-badge.svg\"")
            }}
    }

    override val routes = setOf(findLatestVersion, findLatestDetails, findLatestFile, latestBadge)

}