package com.reposilite.javadocs

import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.javadocs.container.JavadocContainer
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFoundError
import io.javalin.http.ContentType
import panda.std.Result
import panda.std.asError
import panda.std.asSuccess
import panda.utilities.StringUtils
import java.nio.file.Files
import java.nio.file.Path

internal interface JavadocPage {

    fun render(): Result<JavadocResponse, ErrorResponse>

    class JavadocContainerPage(private val container: JavadocContainer) : JavadocPage {
        override fun render(): Result<JavadocResponse, ErrorResponse> {
            val content = this.readFile(container.javadocContainerIndex)

            return JavadocResponse(ContentType.HTML, content).asSuccess()
        }
    }

    class JavadocErrorPage(private val error: ErrorResponse) : JavadocPage {
        override fun render(): Result<JavadocResponse, ErrorResponse> {
            return error.asError()
        }
    }

    class JavadocPlainFilePage(private val javadocPlainFile: JavadocPlainFile) : JavadocPage {
        override fun render(): Result<JavadocResponse, ErrorResponse> {
            return try {
                val contentType = javadocPlainFile.contentType
                val content = this.readFile(javadocPlainFile.targetPath)

                JavadocResponse(contentType.mimeType, content).asSuccess()
            } catch (noSuchFileException: NoSuchFileException) {
                notFoundError("Resource not found!")
            }
        }
    }

    class JavadocEmptyFilePage(private val javadocPlainFile: JavadocPlainFile) : JavadocPage {
        override fun render(): Result<JavadocResponse, ErrorResponse> {
            return JavadocResponse(javadocPlainFile.contentType.mimeType, StringUtils.EMPTY).asSuccess()
        }
    }

    fun readFile(indexFile: Path): String =
        Files.readAllLines(indexFile).joinToString(separator = "\n")

}