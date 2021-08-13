package com.reposilite.frontend.infrastructure

import com.reposilite.frontend.FrontendFacade
import com.reposilite.shared.getSimpleName
import com.reposilite.shared.inputStream
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.routing.RouteMethod.GET
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isDirectory

internal class CustomFrontendHandler(frontendFacade: FrontendFacade, directory: Path) : FrontendHandler(frontendFacade) {

    override val routes: Set<ReposiliteRoute> = Files.list(directory)
        .map {
            if (it.isDirectory())
                ReposiliteRoute("/${it.getSimpleName()}/<path>", GET) {
                    response = respondWithFile(ctx, it.getSimpleName(), it.resolve(ctx.pathParam("path")).inputStream().orNull())
                }
            else
                ReposiliteRoute("/${it.getSimpleName()}", GET) {
                    response = respondWithFile(ctx, it.getSimpleName(), it.inputStream().orNull())
                }
        }
        .collect(Collectors.toSet())
        .also { list ->
            ReposiliteRoute("/", GET) {
                response = respondWithFile(ctx, "index.html", directory.resolve("index.html").inputStream().orNull())
            }.let { list.add(it) }
        }

}