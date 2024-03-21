package com.reposilite.redirect

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.storage.api.Location
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.RoutingSetupEvent
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.HEAD

@Plugin(name = "redirect", dependencies = ["local-configuration", "frontend", "maven"])
class RedirectPlugin : ReposilitePlugin() {

    private val redirectTo: String? = System.getProperty("reposilite.redirect.default-repository", "")

    override fun initialize(): Facade? {
        if (redirectTo.isNullOrEmpty()) {
            return null
        }

        val mavenFacade = facade<MavenFacade>()

        val mavenEndpoints = MavenEndpoints(
            mavenFacade = mavenFacade,
            frontendFacade = facade<FrontendFacade>(),
            compressionStrategy = facade<LocalConfiguration>().compressionStrategy.get()
        )

        logger.info("")
        logger.info("--- Redirect")

        val redirectedRoutes = mavenFacade.getRepository(redirectTo)
            ?.storageProvider
            ?.getFiles(Location.of("/"))
            ?.orNull()
            ?.map {
                logger.info("Redirecting /${it.getSimpleName()}/<gav> to /$redirectTo/${it.getSimpleName()}/<gav>")

                ReposiliteRoute<Unit>("/${it.getSimpleName()}/<gav>", HEAD, GET) {
                    accessed {
                        mavenEndpoints.findFile(
                            ctx = ctx,
                            identifier = this?.identifier,
                            repository = redirectTo,
                            gav = it.resolve(requireParameter("gav"))
                        )
                    }
                }
            }
            ?: emptyList()

        event { event: RoutingSetupEvent ->
            event.register(redirectedRoutes)
        }

        return null
    }

}