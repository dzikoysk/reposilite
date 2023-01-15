package com.reposilite.javadocs.page

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import java.nio.file.Files
import java.nio.file.Path

internal class JavadocPageFactory(
    private val mavenFacade: MavenFacade,
    private val javadocFolder: Path,
    ) {

    fun createPage(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): JavadocPage {
        val repositoryFile = JavadocPlainFile.create(javadocFolder, repository, gav)

        if (repositoryFile != null) {
            if (!Files.exists(repositoryFile.targetPath)) {
                return JavadocPlainEmptyFilePage(repositoryFile)
            }

            return JavadocPlainFilePage(repositoryFile)
        }

        return JavadocJarPage(mavenFacade, javadocFolder, accessToken, repository, gav)
    }

}