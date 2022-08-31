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

import com.reposilite.maven.MatchedVersionHandler
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.LatestArtifactQuery
import com.reposilite.maven.api.LatestArtifactQueryRequest
import com.reposilite.maven.api.LatestBadgeRequest
import com.reposilite.maven.api.LatestVersionResponse
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequestError
import com.reposilite.shared.extensions.contentDisposition
import com.reposilite.shared.extensions.resultAttachment
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.toLocation
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.ContentType
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import panda.std.Result
import panda.std.asSuccess

internal class MavenLatestApiEndpoints(
    mavenFacade: MavenFacade,
    private val compressionStrategy: String
) : MavenRoutes(mavenFacade) {

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/version/{repository}/{gav}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
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
            requireGav { gav ->
                requireRepository { repository ->
                    response = VersionLookupRequest(this?.identifier, repository, gav, ctx.queryParam("filter"))
                        .let { mavenFacade.findLatestVersion(it) }
                        .flatMap {
                            when (val type = ctx.queryParam("type")) {
                                "raw" -> it.version.asSuccess() // -> String
                                "json", null -> it.asSuccess() // -> LatestVersionResponse
                                else -> badRequestError("Unsupported response type: $type") // -> ErrorResponse
                            }
                        }
                }
            }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/details/{repository}/{gav}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [
            OpenApiParam(name = "extension", description = "Changes extension of matched file (by default matches 'jar')", required = false),
            OpenApiParam(name = "classifier", description = "Appends classifier suffix to matched file", required = false),
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
        ],
        responses = [ OpenApiResponse("200", description = "Details about the given file", content = [OpenApiContent(from = FileDetails::class)]) ]
    )
    private val findLatestDetails = ReposiliteRoute("/api/maven/latest/details/{repository}/<gav>", GET) {
        accessed {
            requireRepository {
                response = resolveLatestArtifact(
                    context = this@ReposiliteRoute,
                    accessToken = this,
                    repository = it,
                    handler = { lookupRequest -> mavenFacade.findDetails(lookupRequest) }
                )
            }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/file/{repository}/{gav}",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        queryParams = [
            OpenApiParam(name = "extension", description = "Changes extension of matched file (by default matches 'jar')", required = false),
            OpenApiParam(name = "classifier", description = "Appends classifier suffix to matched file", required = false),
            OpenApiParam(name = "filter", description = "Version (prefix) filter to apply", required = false),
        ]
    )
    private val findLatestFile = ReposiliteRoute<Unit>("/api/maven/latest/file/{repository}/<gav>", GET) {
        accessed {
            requireRepository {
                response = resolveLatestArtifact(
                    context = this@ReposiliteRoute,
                    accessToken = this,
                    repository = it,
                    handler = { lookupRequest ->
                        mavenFacade.findFile(lookupRequest).map { (details, file) ->
                            ctx.resultAttachment(details.name, details.contentType, details.contentLength, compressionStrategy, file)
                        }
                    }
                )
            }
        }
    }

    private fun <T> resolveLatestArtifact(context: ContextDsl<*>, accessToken: AccessTokenDto?, repository: Repository, handler: MatchedVersionHandler<T>): Result<T, ErrorResponse> =
        mavenFacade.findLatestVersionFile(
            LatestArtifactQueryRequest(
                accessToken = accessToken?.identifier,
                repository = repository,
                query = LatestArtifactQuery(
                    gav = context.requireParameter("gav").toLocation(),
                    extension = context.queryParameter("extension") ?: "jar",
                    classifier = context.queryParameter("classifier"),
                    filter = context.ctx.queryParam("filter"),
                )
            ),
            handler = handler
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
        accessed {
            requireGav { gav ->
                requireRepository { repository ->
                    response =
                        mavenFacade.createLatestBadge(
                            LatestBadgeRequest(
                                accessToken = this?.identifier,
                                repository = repository,
                                gav = gav,
                                name = queryParameter("name"),
                                color = queryParameter("color"),
                                prefix = queryParameter("prefix"),
                                filter = queryParameter("filter")
                            )
                        ).peek {
                            ctx.contentType("image/svg+xml")
                            ctx.contentDisposition("inline; filename=\"latest-badge.svg\"")
                        }
                }
            }
        }
    }

    override val routes = routes(findLatestVersion, findLatestDetails, findLatestFile, latestBadge)

}
