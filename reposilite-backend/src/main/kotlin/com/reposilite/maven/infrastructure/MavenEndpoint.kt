package com.reposilite.maven.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.DirectoryInfo
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.LookupRequest
import com.reposilite.web.ReposiliteRoute
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.ReposiliteWebDsl
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.resultAttachment
import com.reposilite.web.routing.RouteMethod.DELETE
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.HEAD
import com.reposilite.web.routing.RouteMethod.POST
import com.reposilite.web.routing.RouteMethod.PUT
import io.javalin.http.HttpCode.NO_CONTENT
import io.javalin.openapi.ContentType.FORM_DATA_MULTIPART
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse

internal class MavenEndpoint(
    private val mavenFacade: MavenFacade,
    private val frontendFacade: FrontendFacade
) : ReposiliteRoutes() {

    @OpenApi(
        tags = ["Maven"],
        path = "/{repository}/*",
        methods = [HttpMethod.GET],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "404", description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository")
        ]
    )
    private val findFile = ReposiliteRoute("/{repository}/<gav>", HEAD, GET) {
        accessed {
            response = mavenFacade.findFile(LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken))
                .filter({ it is DocumentInfo }, { ErrorResponse(NO_CONTENT, "Requested file is a directory")})
                .map { it as DocumentInfo }
                .peek { ctx.resultAttachment(it.name, it.contentType, it.contentLength, it.content()) }
                .onError { ctx.html(frontendFacade.createNotFoundPage(context.uri)) }
        }
    }

    @OpenApi(
        tags = [ "Maven" ],
        path = "/{repository}/*",
        methods = [HttpMethod.POST, HttpMethod.PUT],
        summary = "Deploy artifact to the repository",
        description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "401", description = "Returns 401 for invalid credentials"),
            OpenApiResponse(status = "405", description = "Returns 405 if deployment is disabled in configuration"),
            OpenApiResponse(status = "507", description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file")
        ]
    )
    private val deployFile = ReposiliteRoute("/{repository}/<gav>", POST, PUT) {
        authorized {
            response = mavenFacade.deployFile(DeployRequest(parameter("repository"), parameter("gav"), getSessionIdentifier(), context.input()))
                .onError { context.logger.debug("Cannot deploy artifact due to: ${it.message}") }
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/{repository}/*",
        summary = "Delete the given file from repository",
        methods = [HttpMethod.DELETE],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
        ]
    )
    private val deleteFile = ReposiliteRoute("/{repository}/<gav>", DELETE) {
        authorized {
            response = mavenFacade.deleteFile(DeleteRequest(accessToken, parameter("repository"), parameter("gav")))
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/details",
        methods = [HttpMethod.GET],
        summary = "Get list of available repositories",
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns list of available repositories",
                content = [OpenApiContent(from = DirectoryInfo::class)]
            )
        ]
    )
    private val findRepositories = ReposiliteRoute("/api/maven/details", GET) {
        accessed {
            response = mavenFacade.findFile(LookupRequest(null, "", this?.accessToken))
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/details/{repository}/*",
        methods = [HttpMethod.GET],
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Returns document (different for directory and file) that describes requested resource",
                content = [OpenApiContent(from = FileDetails::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Returns 401 in case of unauthorized attempt of access to private repository",
                content = [OpenApiContent(from = ErrorResponse::class)]
            ),
            OpenApiResponse(
                status = "404",
                description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
            )
        ]
    )
    private val findFileDetails: suspend ReposiliteWebDsl.() -> Unit = {
        accessed {
            response = mavenFacade.findFile(LookupRequest(parameter("repository"), wildcard("gav"), this?.accessToken))
            println(response)
        }
    }

    private val findFileDetailsWithoutGav = ReposiliteRoute("/api/maven/details/{repository}", GET, handler = findFileDetails)
    private val findFileDetailsWithGav = ReposiliteRoute("/api/maven/details/{repository}/<gav>", GET, handler = findFileDetails)

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/versions/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
    )
    private val findVersions = ReposiliteRoute("/api/maven/versions/{repository}/<gav>", GET) {
        accessed {
            response = mavenFacade.findVersions(LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken))
        }
    }

    @OpenApi(
        tags = ["Maven"],
        path = "/api/maven/latest/{repository}/*",
        methods = [HttpMethod.GET],
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
    )
    private val findLatest = ReposiliteRoute("/api/maven/latest/{repository}/<gav>", GET) {
        accessed {
            response = mavenFacade.findLatest(LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken))
        }
    }

    override val routes = setOf(findFile, deployFile, deleteFile, findRepositories, findFileDetailsWithoutGav, findFileDetailsWithGav, findVersions, findLatest)

}