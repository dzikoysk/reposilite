package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.FilesUtils.getExtension
import com.reposilite.shared.decodeToString
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.safeResolve
import com.reposilite.web.ReposiliteRoute
import com.reposilite.web.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.encoding
import com.reposilite.web.http.errorResponse
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.http.ContentType
import io.javalin.http.ContentType.Companion.PLAIN
import io.javalin.http.Context
import io.javalin.http.HttpCode.NOT_FOUND
import panda.std.Result
import panda.std.Result.ok
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isDirectory
import kotlin.text.Charsets.UTF_8

internal sealed class FrontendHandler(protected val frontendFacade: FrontendFacade) : ReposiliteRoutes() {

    protected fun respondWithFile(ctx: Context, uri: String, source: () -> String?): Result<String, ErrorResponse> =
        frontendFacade.resolve(uri, source)
            ?.let {
                ctx.encoding(UTF_8)
                ctx.contentType(ContentType.getMimeTypeByExtension(uri.getExtension()) ?: PLAIN)
                ok(it)
            }
            ?: errorResponse(NOT_FOUND, "Resource not found");

}

internal class ResourcesFrontendHandler(frontendFacade: FrontendFacade) : FrontendHandler(frontendFacade) {

    private val defaultHandler = ReposiliteRoute("/", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val indexHandler = ReposiliteRoute("/index.html", GET) {
        response = respondWithResource(ctx, "index.html")
    }

    private val assetsHandler = ReposiliteRoute("/assets/<path>", GET) {
        response = respondWithResource(ctx, "assets/${ctx.pathParam("path")}")
    }

    private fun respondWithResource(ctx: Context, uri: String): Result<String, ErrorResponse> =
        respondWithFile(ctx, uri, { FrontendFacade::class.java.getResourceAsStream("/static/$uri")?.readBytes()?.decodeToString() })

    override val routes =
        setOf(defaultHandler, indexHandler, assetsHandler)

}

internal class CustomFrontendHandler(frontendFacade: FrontendFacade, directory: Path) : FrontendHandler(frontendFacade) {

    override val routes: Set<ReposiliteRoute> = run {
        val routes = Files.list(directory)
            .map {
                if (it.isDirectory()) ReposiliteRoute("/${it.getSimpleName()}/<path>", GET) {
                    response = respondWithFile(ctx, it.getSimpleName()) { it.safeResolve(ctx.pathParam("path")).decodeToString().orNull() }
                }
                else ReposiliteRoute("/${it.getSimpleName()}", GET) {
                    response = respondWithFile(ctx, it.getSimpleName()) { it.decodeToString().orNull() }
                }
            }
            .collect(Collectors.toSet())

        routes.add(ReposiliteRoute("/", GET) {
            response = respondWithFile(ctx, "index.html") { directory.safeResolve("index.html").decodeToString().orNull() }
        })

        routes
    }

}