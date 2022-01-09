package com.reposilite.plugin.javadoc

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Dec. 27, 2021
 */
internal class DocExtractor {

    @Throws(Exception::class)
    fun extractJavadoc(jarFilePath: Path, destination: Path) {
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
            extractJavadoc(destination.toFile(), jarFile)
        }
    }

    @Throws(IOException::class)
    private fun extractJavadoc(destination: File, jarFile: JarFile) {
        val entries = jarFile.entries()

        entries.asSequence().forEach { file ->
            val javaFile = File(destination.absolutePath.toString() + File.separator + file.name )
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

    }
}