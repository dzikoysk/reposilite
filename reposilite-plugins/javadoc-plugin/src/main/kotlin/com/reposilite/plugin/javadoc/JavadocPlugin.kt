package com.reposilite.plugin.javadoc

import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.javadoc.infrastructure.JavadocEndpoints
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "javadoc")
class JavadocPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        val javadocFolder = extensions().parameters.workingDirectory.resolve("javadocs")
        val mavenFacade: MavenFacade = extensions().facade()
        val facade = JavadocFacade(javadocFolder, mavenFacade, this)

        event { event: RoutingSetupEvent ->
            event.registerRoutes(JavadocEndpoints(facade))
        }

        return facade
    }

}