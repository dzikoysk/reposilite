package com.reposilite.javadocs.page

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import java.nio.file.Path

internal class JavadocContainer(
    val javadocContainerIndex: Path,
    val javadocContainerPath: Path,
    val javadocCachePath: Path
) {

    companion object {
        private const val DIR_SEPARATOR = "/"

        private const val CONTAINER_DOCS_DIR = "container"
        private const val INDEX_DOCS_HTML = "index.html"
        private const val CACHE_DOCS_DIR = "cache"

        fun create(javadocFolder: Path, repository: Repository, gav: Location): JavadocContainer {
            val javadocContainerPath = javadocFolder
                .resolve(repository.name)
                .resolve(gav.locationBeforeLast(DIR_SEPARATOR).toString())
                .resolve(CONTAINER_DOCS_DIR)

            val javadocContainerIndex = javadocContainerPath.resolve(INDEX_DOCS_HTML)
            val javadocCachePath = javadocContainerPath.resolve(CACHE_DOCS_DIR)

            return JavadocContainer(javadocContainerIndex, javadocContainerPath, javadocCachePath)
        }

    }

}