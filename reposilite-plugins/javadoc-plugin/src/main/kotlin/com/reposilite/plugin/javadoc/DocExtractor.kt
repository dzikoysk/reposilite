package com.reposilite.plugin.javadoc

import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.HttpCode
import panda.std.Result
import panda.std.Unit
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

internal class DocExtractor {

    fun extractJavadoc(jarFilePath: Path, destination: Path): Result<Unit, ErrorResponse> {
        val file = jarFilePath.toFile()
        val dest = destination.toFile()

        // Some checks, to make sure we're working with valid files/paths.
        require(!file.isDirectory) { "JavaDoc jar file must not be a directory!" }
        require(dest.isDirectory) { "Destination must be a directory!" }
        check(file.name.contains("doc.jar")) { "Invalid javadoc.jar! Name must contain: \"doc.jar\"" }

        JarFile(file).use { jarFile ->
            // Making sure we have an index.html file
            val entry = jarFile.getEntry("index.html")
            check(!(entry == null || entry.isDirectory)) { "Invalid doc.jar!" }

            val result = extractJavadoc(destination.toFile(), jarFile)
            file.delete()
            return result
        }
    }

    private fun extractJavadoc(destination: File, jarFile: JarFile): Result<Unit, ErrorResponse> {
        val entries = jarFile.entries()

        try {
            entries.asSequence().forEach { file ->
                val javaFile = File(destination.absolutePath.toString() + File.separator + file.name)
                if (file.isDirectory) {
                    javaFile.mkdir()
                } else {
                    jarFile.getInputStream(file).use { input ->
                        javaFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            return Result.ok()
        } catch (e: Exception) {
            return errorResponse(HttpCode.INTERNAL_SERVER_ERROR, e.message.orEmpty())
        }
    }
}