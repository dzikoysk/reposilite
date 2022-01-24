package com.reposilite.plugin.javadoc

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.javadoc.api.JavadocResponse
import com.reposilite.plugin.javadoc.api.JavadocPageRequest
import com.reposilite.storage.api.FileType.FILE
import com.reposilite.storage.api.Location
import com.reposilite.storage.getSimpleName
import com.reposilite.token.api.AccessTokenDto
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import com.reposilite.web.http.notFound
import com.reposilite.web.http.notFoundError
import com.reposilite.web.http.unauthorizedError
import io.javalin.http.ContentType
import io.javalin.http.HttpCode.BAD_REQUEST
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.outputStream

class JavadocFacade internal constructor(
    private val javadocFolder: Path,
    private val mavenFacade: MavenFacade,
    private val journalist: Journalist
) : Journalist, Facade {

    fun findJavadocPage(request: JavadocPageRequest): Result<JavadocResponse, ErrorResponse> {
        val (repository, gav, accessToken) = request

        if (!mavenFacade.canAccessResource(accessToken?.identifier, repository, gav)) {
            return unauthorizedError()
        }

        val target = javadocFolder.resolve(repository).resolve(gav.toString())

        if (Files.exists(target) && (gav.getExtension() == "html" || gav.getExtension() == "css" || gav.getExtension() == "js")) {
            return try {
                val contentType = ContentType.getMimeTypeByExtension(gav.getExtension()) ?: ContentType.PLAIN
                val response = Files.readAllLines(target).joinToString(separator = "\n")
                JavadocResponse(contentType, response).asSuccess()
            } catch (noSuchFileException: NoSuchFileException) {
                notFoundError("Resource not found!")
            }
        }

        if (gav.contains("/resources/")) {
            return notFoundError("Resources are unavailable before extraction")
        }

        val javadocJar = resolveJavadocJar(gav) ?: return notFoundError("Invalid request")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository, javadocJar))
            .filter({ it.type === FILE }, { ErrorResponse(BAD_REQUEST, "Invalid request") })
            .filter({ it.name.endsWith("-javadoc.jar") }, { notFound("Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!") })
            .flatMap { extractJavadocJar(javadocJar, repository, javadocFolder, accessToken) }
            .map { JavadocResponse(ContentType.HTML, it) }
            .onError { logger.error("Cannot extract javadoc: ${it.message} (${it.status})}") }
    }

    /**
     * Constructs a new gav from the input gav, so we can handle paths that end with /index.html for example.
     */
    private fun resolveJavadocJar(gav: Location): Location? =
        when {
            gav.endsWith(".jar") -> gav
            gav.endsWith("/index.html") -> {
                val path = gav.locationBeforeLast("/index.html")
                val split = path.toString().split("/")

                if (split.size >= 2) {
                    val version = split[split.size - 1]
                    val name = split[split.size - 2]
                    path.resolve("${name}-${version}-javadoc.jar")
                }
                else null
            }
            else -> resolveJavadocJar(gav.resolve("index.html"))
        }

    /**
     * Retrieves the javadoc jar file from mavenFacade using a LookupRequest.
     * Then it will extract the files from the jar file, rename index.html to docindex.html and generate a new index.html file using the writeNewIndex method.
     *
     * @param gav the direct gav to the javadoc file
     * @param javadocFolder the target folder in which the files should be extracted
     */
    private fun extractJavadocJar(gav: Location, repository: String, javadocFolder: Path, token: AccessTokenDto?): Result<String, ErrorResponse> {
        val path = gav.locationBeforeLast("/").toString()
        val targetFolder = javadocFolder.resolve(repository).resolve(path)
        val indexFile = targetFolder.resolve("index.html")

        if (Files.exists(indexFile)) {
            return ok(Files.readAllLines(indexFile).joinToString(separator = "\n"))
        }

        Files.createDirectories(targetFolder)
        val targetJar = targetFolder.resolve("javadoc.jar")

        return mavenFacade.findFile(LookupRequest(token, repository, gav))
            .peek { it.copyToAndClose(FileOutputStream(targetJar.toString())) }
            .flatMap { extractJavadocArchive(targetJar, targetFolder) }
            .map {
                Files.move(indexFile, targetFolder.resolve("docindex.html"))
                Files.write(indexFile, JavadocView.index().toByteArray(Charsets.UTF_8))
                Files.readAllLines(indexFile).joinToString(separator = "\n")
            }
    }

    private fun extractJavadocArchive(jarFilePath: Path, destination: Path): Result<Unit, ErrorResponse> =
        when {
            Files.isDirectory(jarFilePath) -> errorResponse(BAD_REQUEST, "JavaDoc jar path has to be a file!")
            !Files.isDirectory(destination) -> errorResponse(BAD_REQUEST, "Destination must be a directory!")
            !jarFilePath.getSimpleName().contains("javadoc.jar") -> errorResponse(BAD_REQUEST, "Invalid javadoc jar! Name must contain: 'javadoc.jar'")
            else -> jarFilePath.toAbsolutePath().toString()
                .let { JarFile(it) }
                .use { jarFile ->
                    if (jarFile.getEntry("index.html")?.isDirectory != false) { // Make sure we have an index.html file
                        return errorResponse(INTERNAL_SERVER_ERROR, "Invalid javadoc.jar given for extraction")
                    }

                    jarFile.entries().asSequence().forEach { file ->
                        Paths.get(destination.toString() + "/" + file.name).also {
                            if (!file.isDirectory) {
                                it.parent?.also { parent -> Files.createDirectories(parent) }
                                jarFile.getInputStream(file).copyToAndClose(it.outputStream())
                            }
                        }
                    }.asSuccess<Unit, ErrorResponse>()
                }
                .also { Files.deleteIfExists(jarFilePath) }
        }

    private fun InputStream.copyToAndClose(output: OutputStream) =
        this.use { output.use { this.copyTo(output) } }

    override fun getLogger(): Logger =
        journalist.logger

}