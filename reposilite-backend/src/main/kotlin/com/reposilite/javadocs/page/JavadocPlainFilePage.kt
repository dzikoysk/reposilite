package com.reposilite.javadocs.page

import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import io.javalin.http.ContentType
import panda.std.Result
import panda.std.asSuccess
import java.nio.file.Files
import java.nio.file.Path

internal class JavadocPlainFilePage(private val repositoryFile: JavadocPlainFile) : JavadocPage {

    override fun render(): Result<JavadocResponse, ErrorResponse> {
        return this.readPlainFile()
    }

    private fun readPlainFile(): Result<JavadocResponse, ErrorResponse> {
        return try {
            val contentType = ContentType.getMimeTypeByExtension(repositoryFile.extension) ?: ContentType.PLAIN
            val response = this.readFile(repositoryFile.targetPath)

            JavadocResponse(contentType, response).asSuccess()
        } catch (noSuchFileException: NoSuchFileException) {
            notFoundError("Resource not found!")
        }
    }

    private fun readFile(indexFile: Path): String =
        Files.readAllLines(indexFile).joinToString(separator = "\n")

}