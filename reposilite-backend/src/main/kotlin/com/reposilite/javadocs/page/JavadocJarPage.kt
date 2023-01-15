package com.reposilite.javadocs.page

import com.reposilite.javadocs.JavadocView
import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.*
import com.reposilite.storage.api.FileType
import com.reposilite.storage.api.Location
import com.reposilite.storage.getSimpleName
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import io.javalin.http.HttpStatus
import panda.std.Result
import panda.std.asSuccess
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.outputStream

internal class JavadocJarPage(
    private val mavenFacade: MavenFacade,
    private val javadocFolder: Path,
    private val accessToken: AccessTokenIdentifier?,
    private val repository: Repository,
    private val gav: Location,
) : JavadocPage {

    companion object {
        private const val RESOURCES_PATH = "/resources/"
        private const val CACHE_DIR = "cache"

        private const val JAR_EXTENSION = ".jar"
        private const val JAVADOC_JAR_EXTENSION = "-javadoc.jar"
        private const val JAVADOC_JAR = "javadoc.jar"

        private const val SNAPSHOT_TAG = "-SNAPSHOT"
        private const val INDEX_HTML = "index.html"
        private const val INDEX_HTML_PATH = "/index.html"
        private const val INDEX_DOCS_HTML = "index.html"

        private const val DIR_SEPARATOR = "/"

        fun isJavadocJar(path: String) =
            path.endsWith(JAVADOC_JAR_EXTENSION)

        fun isIndexExist(jarFile: JarFile) =
            jarFile.getJarEntry(INDEX_HTML)?.isDirectory == false
    }

    override fun render(): Result<JavadocResponse, ErrorResponse> {
        if (gav.contains(RESOURCES_PATH)) {
            return notFoundError("Resources are unavailable before extraction")
        }

        val javadocJar = this.resolveJavadocJar(gav) ?: return badRequestError("Invalid GAV")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository.name, javadocJar))
            .filter({ it.type === FileType.FILE }, { badRequest("Invalid request") })
            .filter({ isJavadocJar(it.name) }, { notFound("Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!") })
            .flatMap { extractJavadocJar(javadocJar) }
            .map { JavadocResponse(ContentType.HTML, it) }
    }

    /**
     * Constructs a new gav from the input gav, so we can handle paths that end with /index.html for example.
     */
    private fun resolveJavadocJar(gav: Location): Location? = when {
        gav.endsWith(JAR_EXTENSION) -> gav
        gav.endsWith(INDEX_HTML_PATH) -> resolveIndexGav(gav)
        else -> resolveJavadocJar(gav.resolve(INDEX_HTML))
    }

    private fun resolveIndexGav(gav: Location): Location? {
        val rootGav = gav.locationBeforeLast(INDEX_HTML_PATH)
        val elements = rootGav.toString().split(DIR_SEPARATOR)

        if (elements.size < 2) {
            return null
        }

        val name = elements[elements.size - 2]
        var version = elements[elements.size - 1]

        if (version.contains(SNAPSHOT_TAG)) {
            val metadataResult = mavenFacade.findMetadata(repository, rootGav)
            val snapshot = if (metadataResult.isOk) metadataResult.get().versioning?.snapshot else null

            if (snapshot?.timestamp != null && snapshot.buildNumber != null) {
                version = "${version.replace(SNAPSHOT_TAG, "")}-${snapshot.timestamp}-${snapshot.buildNumber}"
            }
        }

        return rootGav.resolve("${name}-${version}${JAVADOC_JAR_EXTENSION}")
    }

    /**
     * Retrieves the javadoc jar file from mavenFacade using a LookupRequest.
     * Then it will extract the files from the jar file, rename index.html to docindex.html and generate a new index.html file using the writeNewIndex method.
     *
     * @param gav the direct gav to the javadoc file
     */
    private fun extractJavadocJar(gav: Location): Result<String, ErrorResponse> {
        val destinationPath = javadocFolder
            .resolve(repository.name)
            .resolve(gav.locationBeforeLast(DIR_SEPARATOR).toString())

        val indexFile = destinationPath.resolve(INDEX_DOCS_HTML)

        if (Files.exists(indexFile)) {
            return Result.ok(this.readFile(indexFile))
        }

        val cacheDestinationPath = destinationPath.resolve(CACHE_DIR)

        Files.createDirectories(cacheDestinationPath)
        val cacheDestinationJarPath = cacheDestinationPath.resolve(JAVADOC_JAR)

        return mavenFacade.findFile(LookupRequest(accessToken, repository.name, gav))
            .peek { (_, originInput) -> this.copyJavadocJar(originInput, cacheDestinationJarPath) }
            .flatMap { this.unpackJavadocJar(cacheDestinationJarPath, cacheDestinationPath) }
            .peek { this.createDocIndexHtml(destinationPath) }
            .map { this.readFile(indexFile) }
    }

    private fun copyJavadocJar(originInput: InputStream, destinationJarPath: Path) {
        originInput.copyToAndClose(FileOutputStream(destinationJarPath.toString()))
    }

    private fun unpackJavadocJar(destinationJarPath: Path, destinationPath: Path): Result<Unit, ErrorResponse> =
        when {
            !destinationJarPath.getSimpleName().contains(JAVADOC_JAR) -> badRequestError("Invalid javadoc jar! Name must contain: '${JAVADOC_JAR}'")
            Files.isDirectory(destinationJarPath) -> badRequestError("JavaDoc jar path has to be a file!")
            !Files.isDirectory(destinationPath) -> badRequestError("Destination must be a directory!")

            else -> destinationJarPath.toAbsolutePath().toString()
                .let { JarFile(it) }
                .use { jarFile ->
                    if (!isIndexExist(jarFile)) {
                        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid javadoc.jar given for extraction")
                    }

                    jarFile.entries().asSequence().forEach { file ->
                        if (file.isDirectory) {
                            return@forEach
                        }

                        val path = Paths.get(destinationPath.toString() + DIR_SEPARATOR + file.name)

                        path.parent?.also { parent -> Files.createDirectories(parent) }
                        jarFile.getInputStream(file).copyToAndClose(path.outputStream())
                    }.asSuccess<Unit, ErrorResponse>()
                }
                .also { Files.deleteIfExists(destinationJarPath) }
        }

    private fun createDocIndexHtml(destinationPath: Path) {
        val docIndexFile = destinationPath.resolve(INDEX_DOCS_HTML)

        Files.write(docIndexFile, JavadocView.index().toByteArray(Charsets.UTF_8))
    }

    private fun InputStream.copyToAndClose(output: OutputStream) =
        this.use { output.use { this.copyTo(output) } }

    private fun readFile(indexFile: Path): String =
        Files.readAllLines(indexFile).joinToString(separator = "\n")

}