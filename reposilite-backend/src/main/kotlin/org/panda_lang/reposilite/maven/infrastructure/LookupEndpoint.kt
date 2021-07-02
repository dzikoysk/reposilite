package org.panda_lang.reposilite.maven.infrastructure

import com.dzikoysk.openapi.annotations.ContentType.FORM_DATA_MULTIPART
import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.annotations.OpenApi
import com.dzikoysk.openapi.annotations.OpenApiContent
import com.dzikoysk.openapi.annotations.OpenApiParam
import com.dzikoysk.openapi.annotations.OpenApiResponse
import io.javalin.http.Context
import org.panda_lang.reposilite.maven.MavenFacade
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.panda_lang.reposilite.web.api.RouteHandler
import org.panda_lang.reposilite.web.api.RouteMethod.GET
import org.panda_lang.reposilite.web.context
import org.panda_lang.reposilite.web.resultAttachment
import org.panda_lang.utilities.commons.function.Result

private const val ROUTE = "/:repositoryName/*"

internal class LookupEndpoint(
    private val contextFactory: ReposiliteContextFactory,
    private val mavenFacade: MavenFacade
) : RouteHandler {

    override val route = ROUTE
    override val methods = listOf(GET)

    @OpenApi(
        path = ROUTE,
        methods = [HttpMethod.GET],
        operationId = "repositoryLookup",
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        tags = ["Repository"],
        pathParams = [ OpenApiParam(
            name = "*",
            description = "Artifact path qualifier",
            required = true,
            allowEmptyValue = true
            ), OpenApiParam(
            name = "*/latest",
            description = "[Optional] Artifact path qualifier with /latest at the end returns latest version of artifact as text/plain"
        )],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Input stream of requested file",
                content = [OpenApiContent(type = FORM_DATA_MULTIPART)]
            ), OpenApiResponse(
                status = "404",
                description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository"
            )
        ]
    )
    override fun handle(ctx: Context) = context(contextFactory, ctx) {
        context.logger.info("Lookup API ${context.uri} from ${context.address}")

        accessed {
            val request = LookupRequest(parameter("repositoryName"), wildcard(), this?.getSessionIdentifier() ?: context.address, this?.accessToken)

            mavenFacade.lookup(request)
                .peek {
                    it.data?.let { data ->
                        ctx.resultAttachment(it.fileDetails, data)
                    }
                }
                .onError { response = Result.error(it) }
        }
    }

}