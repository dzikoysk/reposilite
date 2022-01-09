package com.reposilite.plugin.javadoc

import com.reposilite.Reposilite
import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposiliteInitializeEvent
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.web.api.RoutingSetupEvent
import java.io.File

@Plugin(name = "JavadocPlugin")
class JavadocPlugin : ReposilitePlugin() {

    private lateinit var reposilite: Reposilite
    private lateinit var javadocFolder: File

    override fun initialize(): Facade? {
        extensions().registerEvent(ReposiliteInitializeEvent::class.java) { event: ReposiliteInitializeEvent ->
            logger.info("")
            logger.info("--- javadoc plugin")
            logger.info("JavaDoc plugin has been successfully loaded!")

            reposilite = event.reposilite
            javadocFolder = File(reposilite.parameters.workingDirectory.toFile().absolutePath + File.separator + "javadocs")
            if (!javadocFolder.exists()) {
                javadocFolder.mkdir();
            }
        }

        val mavenFacade: MavenFacade = extensions().facade()

        extensions().registerEvent(RoutingSetupEvent::class.java) { event: RoutingSetupEvent ->
            event.registerRoutes(JavadocRoute(mavenFacade, javadocFolder))
        }

        return null
    }
}