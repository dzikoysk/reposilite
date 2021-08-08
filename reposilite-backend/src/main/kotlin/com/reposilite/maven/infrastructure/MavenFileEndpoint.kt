package com.reposilite.maven.infrastructure

import com.reposilite.failure.api.ErrorResponse
import com.reposilite.failure.api.errorResponse
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.DocumentInfo
import com.reposilite.maven.api.FileDetails
import com.reposilite.maven.api.LookupRequest
import com.reposilite.web.api.MimeTypes.MULTIPART_FORM_DATA
import com.reposilite.web.api.Route
import com.reposilite.web.api.RouteMethod.DELETE
import com.reposilite.web.api.RouteMethod.GET
import com.reposilite.web.api.RouteMethod.HEAD
import com.reposilite.web.api.RouteMethod.POST
import com.reposilite.web.api.RouteMethod.PUT
import com.reposilite.web.api.Routes
import com.reposilite.web.resultAttachment
import io.javalin.http.HttpCode.NO_CONTENT
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import panda.std.Result

internal class MavenFileEndpoint(private val mavenFacade: MavenFacade) : Routes {

    @OpenApi(
        path = "/{repository}/*",
        methods = [HttpMethod.GET],
        tags = ["Maven"],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = MULTIPART_FORM_DATA)]),
            OpenApiResponse(status = "404", description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository")
        ]
    )
    val findFile = Route("/{repository}/<gav>", GET) {
        accessed {
            LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken)
                .let { mavenFacade.findFile(it) }
                .peek {
                    when (it) {
                        is DocumentInfo -> ctx.resultAttachment(it)
                        else -> response = errorResponse(NO_CONTENT, "Requested file is a directory")
                    }
                }
                .onError { response = Result.error(it) }
        }
    }

    @OpenApi(
        path = "/{repository}/*",
        methods = [HttpMethod.POST, HttpMethod.PUT],
        summary = "Deploy artifact to the repository",
        description = "Deploy supports both, POST and PUT, methods and allows to deploy artifact builds",
        tags = [ "Maven" ],
        pathParams = [
            OpenApiParam(name = "*", description = "Artifact path qualifier", required = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = MULTIPART_FORM_DATA)]),
            OpenApiResponse(status = "401", description = "Returns 401 for invalid credentials"),
            OpenApiResponse(status = "405", description = "Returns 405 if deployment is disabled in configuration"),
            OpenApiResponse(status = "507", description = "Returns 507 if Reposilite does not have enough disk space to store the uploaded file")
        ]
    )
    private val deployFile = Route("/{repository}/<gav>", POST, PUT) {
        authorized {
            response = DeployRequest(parameter("repository"), parameter("gav"), getSessionIdentifier(), context.input())
                .let { mavenFacade.deployFile(it) }
                .onError { context.logger.debug("Cannot deploy artifact due to: ${it.message}") }
        }
    }

    @OpenApi(
        path = "/{repository}/<gav>",
        summary = "Delete the given file from repository",
        methods = [HttpMethod.DELETE]
    )
    private val deleteFile = Route("/{repository}/<gav>", DELETE) {
        authorized {
            response = mavenFacade.deleteFile(DeleteRequest(accessToken, parameter("repository"), parameter("gav")))
        }
    }

    @OpenApi(
        path = "/api/maven/details/{repository}/<gav>",
        methods = [HttpMethod.HEAD, HttpMethod.GET],
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        tags = ["Maven"],
        pathParams = [
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
    val findFileDetails = Route("/api/maven/details/{repository}/<gav>", HEAD, GET) {
        accessed {
            response = LookupRequest(parameter("repository"), parameter("gav"), this?.accessToken)
                .let { mavenFacade.findFile(it) }
        }
    }

    override val routes = setOf(findFile, deployFile, deleteFile, findFileDetails)

}