package com.reposilite.plugin.javadoc

import java.util.jar.JarFile
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.file.Path
import java.util.jar.JarEntry

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Dec. 27, 2021
 */
internal class DocExtractor {

    private val useCommand = true // Whether to use the "jar xf" command or extract programmatically with streams
    private val commandPattern = "jar xf %s"

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

            if (useCommand) extractJavadocViaCommand(jarFilePath, dest)
            else extractJavadocProgrammatically(destination.toFile(), jarFile)
        }
    }

    @Throws(IOException::class)
    private fun extractJavadocViaCommand(file: Path, destination: File) {
        val command = String.format(commandPattern, file)
        // destination is the working directory
        // this will block the current thread, because we only want to proceed once the unpacking finished!
        val process = Runtime.getRuntime().exec(command, null, destination).onExit().join()
        process.destroyForcibly()
    }

    @Throws(IOException::class)
    private fun extractJavadocProgrammatically(destination: File, jarFile: JarFile) {
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val file = File(destination.toString() + File.separator + entry.name)
            if (entry.isDirectory) {
                check(file.mkdir()) { "Could not create directory while extracting JavaDoc programmatically. Entry: " + entry.name }
                continue
            }
            writeToFile(jarFile, entry, file)
        }
    }

    @Throws(IOException::class)
    private fun writeToFile(jar: JarFile, entry: JarEntry, file: File) {
        jar.getInputStream(entry).use { inputStream ->
            FileOutputStream(file).use { fileOutputStream ->
                inputStream.copyTo(fileOutputStream)
            }
        }
    }
}