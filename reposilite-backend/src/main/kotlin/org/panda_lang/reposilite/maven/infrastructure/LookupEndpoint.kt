package org.panda_lang.reposilite.maven.infrastructure

import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.annotations.OpenApi
import com.dzikoysk.openapi.annotations.OpenApiContent
import com.dzikoysk.openapi.annotations.OpenApiParam
import com.dzikoysk.openapi.annotations.OpenApiResponse
import io.javalin.http.HttpCode.NO_CONTENT
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.maven.api.DocumentInfo
import org.panda_lang.reposilite.maven.api.FileDetails
import org.panda_lang.reposilite.maven.api.FileType.FILE
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.web.api.MimeTypes.MULTIPART_FORM_DATA
import org.panda_lang.reposilite.web.api.Route
import org.panda_lang.reposilite.web.api.RouteMethod.GET
import org.panda_lang.reposilite.web.api.RouteMethod.HEAD
import org.panda_lang.reposilite.web.api.Routes
import org.panda_lang.reposilite.web.filter
import org.panda_lang.reposilite.web.resultAttachment
import org.panda_lang.utilities.commons.function.Result

internal class LookupEndpoint(private val mavenFacade: MavenFacade) : Routes {

    @OpenApi(
        path = "/:repositoryName/*",
        methods = [HttpMethod.GET],
        tags = ["Maven"],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Input stream of requested file",
                content = [OpenApiContent(type = MULTIPART_FORM_DATA)]
            ),
            OpenApiResponse(
                status = "404",
                description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository"
            )
        ]
    )
    val findFile = Route("/:repositoryName/*", GET) {
        context.logger.info("Lookup API ${context.uri} from ${context.address}")

        accessed {
            val request = LookupRequest(parameter("repositoryName"), wildcard(), this?.getSessionIdentifier() ?: context.address, this?.accessToken)

            mavenFacade.lookup(request)
                .filter({ it.type == FILE }, { ErrorResponse(NO_CONTENT, "Requested file is a directory") })
                .map { it as DocumentInfo }
                .map { ctx.resultAttachment(it) }
                .onError { response = Result.error(it) }
        }
    }

    @OpenApi(
        path = "/api/:repositoryName/*",
        methods = [HttpMethod.HEAD, HttpMethod.GET],
        summary = "Browse the contents of repositories using API",
        description = "Get details about the requested file as JSON response",
        tags = ["Repository"],
        pathParams = [OpenApiParam(name = "*", description = "Artifact path qualifier", required = true, allowEmptyValue = true)],
        responses = [OpenApiResponse(
            status = "200",
            description = "Returns document (different for directory and file) that describes requested resource",
            content = [OpenApiContent(from = FileDetails::class)]
        ), OpenApiResponse(
            status = "401",
            description = "Returns 401 in case of unauthorized attempt of access to private repository",
            content = [OpenApiContent(from = ErrorResponse::class)]
        ), OpenApiResponse(
            status = "404",
            description = "Returns 404 (for Maven) and frontend (for user) as a response if requested artifact is not in the repository"
        )]
    )
    val findFileDetails = Route("/api/:repositoryName/*", HEAD, GET) {
        context.logger.info("Lookup API ${context.uri} from ${context.address}")

        accessed {
            val request = LookupRequest(parameter("repositoryName"), wildcard(), this?.getSessionIdentifier() ?: context.address, this?.accessToken)
            response = mavenFacade.lookup(request)
        }
    }

    override val routes = setOf(findFile, findFileDetails)

}