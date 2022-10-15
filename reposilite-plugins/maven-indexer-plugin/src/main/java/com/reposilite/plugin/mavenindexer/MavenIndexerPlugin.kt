package com.reposilite.plugin.mavenindexer

import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.mavenindexer.infrastructure.MavenIndexerTestEndpoints
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "maven-indexer", dependencies = ["maven"])
internal class MavenIndexerPlugin : ReposilitePlugin() {
    override fun initialize(): MavenIndexerFacade {
        val mavenIndexerFacade = MavenIndexerFacade()
        val mavenFacade = facade<MavenFacade>()

        event { event: RoutingSetupEvent ->
            event.registerRoutes(MavenIndexerTestEndpoints(mavenIndexerFacade, mavenFacade))
        }

        return mavenIndexerFacade
    }
}
