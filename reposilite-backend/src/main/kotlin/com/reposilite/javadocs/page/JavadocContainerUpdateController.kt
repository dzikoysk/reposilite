package com.reposilite.javadocs.page

import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.EventListener
import java.nio.file.Path

internal class JavadocContainerUpdateController(private val javadocFolder: Path) : EventListener<DeployEvent> {

    override fun onCall(event: DeployEvent) {
        val gav = event.gav
            .takeIf { JavadocJarPage.isJavadocJar(it.toString()) }
            ?: return

        val container = JavadocContainer.create(javadocFolder, event.repository, gav)
        val javadocDirectory = container.javadocContainerPath.toFile()

        if (javadocDirectory.exists()) {
            javadocDirectory.deleteRecursively()
        }
    }

}
