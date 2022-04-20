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

package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MavenFacade.MatchedVersionHandler
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.settings.SettingsFacade
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.toLocation
import com.reposilite.token.api.AccessTokenDto
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
            OpenApiParam(name = "extension", description = "Changes extension of matched file (by default matches 'jar')", required = false),
            OpenApiParam(name = "classifier", description = "Appends classifier suffix to matched file", required = false),
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
            OpenApiParam(name = "type", description = "Format of expected response type: empty (default) for json; 'raw' for plain text", required = false),
        ],
        responses = [
            OpenApiResponse("200", content = [OpenApiContent(from = LatestVersionResponse::class)], description = "default response"),
            OpenApiResponse("200", content = [OpenApiContent(from = String::class, type = ContentType.PLAIN)], description = ""),
        ]
    )
    private val findLatestVersion = ReposiliteRoute<Any>("/api/maven/latest/version/{repository}/<gav>", GET) {
        accessed {
            response = VersionLookupRequest(this?.identifier, requireParameter("repository"), requireParameter("gav").toLocation(), ctx.queryParam("filter"))
                .let { mavenFacade.findLatest(it) }
                .flatMap {
                    when (val type = ctx.queryParam("type")) {
                        "raw" -> it.version.asSuccess() // -> String
                        "json", null -> it.asSuccess() // -> LatestVersionResponse
                        else -> ErrorResponse(BAD_REQUEST, "Unsupported response type: $type").asError() // -> ErrorResponse
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
        queryParams = [
            OpenApiParam(name = "extension", description = "Changes extension of matched file (by default matches 'jar')", required = false),
            OpenApiParam(name = "classifier", description = "Appends classifier suffix to matched file", required = false),
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
        ],
        responses = [ OpenApiResponse("200", description = "Details about the given file", content = [OpenApiContent(from = FileDetails::class)]) ]
    )
    private val findLatestDetails = ReposiliteRoute<FileDetails>("/api/maven/latest/details/{repository}/<gav>", GET) {
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
        queryParams = [
            OpenApiParam(name = "extension", description = "Changes extension of matched file (by default matches 'jar')", required = false),
            OpenApiParam(name = "classifier", description = "Appends classifier suffix to matched file", required = false),
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
        ]
    )
    private val findLatestFile = ReposiliteRoute<Unit>("/api/maven/latest/file/{repository}/<gav>", GET) {
        accessed {
            response = resolveLatestArtifact(this@ReposiliteRoute, this) { lookupRequest ->
                mavenFacade.findDetails(lookupRequest)
                    .`is`(DocumentInfo::class.java) { ErrorResponse(BAD_REQUEST, "Requested file is a directory") }
                    .flatMap { mavenFacade.findFile(lookupRequest).map { data -> it to data } }
                    .map { (details, file) -> ctx.resultAttachment(details.name, details.contentType, details.contentLength, compressionStrategy.get(), file) }
            }
        }
    }

    private fun <T> resolveLatestArtifact(context: ContextDsl<*>, accessToken: AccessTokenDto?, request: MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        mavenFacade.findLatestArtifact(
            accessToken = accessToken?.identifier,
            repository = context.requireParameter("repository"),
            gav = context.requireParameter("gav").toLocation(),
            extension = context.ctx.queryParam("extension") ?: "jar",
            classifier = context.ctx.queryParam("classifier"),
            filter = context.ctx.queryParam("filter"),
            request = request
        )

    @OpenApi(
        path = "/api/badge/latest/{repository}/{gav}", // Rename 'badge/latest' to 'maven/latest/badge'?
        tags = [ "Maven", "Badge" ],
        pathParams = [
            OpenApiParam(name = "repository", description = "Artifact's repository", required = true),
            OpenApiParam(name = "gav", description = "Artifacts' GAV", required = true)
        ],
        methods = [HttpMethod.GET]
    )
    val latestBadge = ReposiliteRoute<String>("/api/badge/latest/{repository}/<gav>", GET) {
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

    override val routes = routes(findLatestVersion, findLatestDetails, findLatestFile, latestBadge)

}