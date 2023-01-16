package com.reposilite.javadocs

import com.reposilite.maven.Repository
import com.reposilite.storage.api.Location
import io.javalin.http.ContentType
import java.nio.file.Path

private val SUPPORTED_PLAIN_EXTENSIONS = mapOf(
    "html" to ContentType.TEXT_HTML,
    "css" to ContentType.TEXT_CSS,
    "js" to ContentType.APPLICATION_JS,
)

internal data class JavadocPlainFile(val targetPath: Path, val extension: String, val contentType: ContentType)

internal fun createPlainFile(javadocFolder: Path, repository: Repository, gav: Location): JavadocPlainFile? {
    val targetPath = javadocFolder.resolve(repository.name).resolve(gav.toString())
    val extension = gav.getExtension()

    val contentType = SUPPORTED_PLAIN_EXTENSIONS[extension]

    if (contentType != null) {
        return JavadocPlainFile(targetPath, extension, contentType)
    }

    return null
}