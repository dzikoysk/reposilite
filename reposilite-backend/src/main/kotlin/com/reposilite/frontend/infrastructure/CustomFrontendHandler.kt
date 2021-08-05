package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.FilesUtils.getSimpleName
import com.reposilite.shared.inputStream
import com.reposilite.web.api.Route
import com.reposilite.web.api.RouteMethod.GET
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isDirectory

internal class CustomFrontendHandler(frontendFacade: FrontendFacade, directory: Path) : FrontendHandler(frontendFacade) {

    override val routes: Set<Route> = Files.list(directory)
        .map {
            if (it.isDirectory())
                Route("/${it.getSimpleName()}/<path>", GET) {
                    respondWithFile(ctx, it.getSimpleName(), it.resolve(ctx.pathParam("path")).inputStream().orNull())
                }
            else
                Route("/${it.getSimpleName()}", GET) {
                    respondWithFile(ctx, it.getSimpleName(), it.inputStream().orNull())
                }
        }
        .collect(Collectors.toSet())
        .also {
            it.add(
                Route("/", GET) {
                    respondWithFile(ctx, "index.html", directory.resolve("index.html").inputStream().orNull())
                }
            )
        }

}