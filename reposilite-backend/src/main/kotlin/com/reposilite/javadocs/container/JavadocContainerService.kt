package com.reposilite.javadocs.container

import com.reposilite.javadocs.JavadocView
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.*
import com.reposilite.storage.api.FileType
import com.reposilite.storage.api.Location
import com.reposilite.storage.getSimpleName
import com.reposilite.token.AccessTokenIdentifier
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

private const val JAVADOC_JAR_EXTENSION = "-javadoc.jar"
private const val JAVADOC_JAR = "javadoc.jar"
private const val JAVADOC_JAR_INDEX = "index.html"

private const val CONTAINER_DIR = "container"
private const val CONTAINER_VIEW_INDEX = "index.html"
private const val CONTAINER_UNPACK_DIR = "cache"

internal class JavadocContainerService(
    private val mavenFacade: MavenFacade,
    private val javadocFolder: Path
) {

    fun loadContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocContainer, ErrorResponse> {
        val javadocJar = this.resolveJavadocJar(repository, gav) ?: return badRequestError("Invalid GAV")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository.name, javadocJar))
            .filter({ it.type === FileType.FILE }, { badRequest("Invalid request") })
            .filter({ isJavadocJar(it.name) }, { notFound("Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!") })
            .flatMap { loadJavadocJarContainer(accessToken, repository, javadocJar) }
    }

    private fun resolveJavadocJar( repository: Repository, gav: Location): Location? = when {
        gav.endsWith(".jar") -> gav
        gav.endsWith("/$JAVADOC_JAR_INDEX") -> resolveIndexGav(repository, gav)
        else -> resolveJavadocJar(repository, gav.resolve(JAVADOC_JAR_INDEX))
    }

    private fun resolveIndexGav(repository: Repository, gav: Location): Location? {
        val rootGav = gav.locationBeforeLast("/$JAVADOC_JAR_INDEX")
        val elements = rootGav.toString().split("/")

        if (elements.size < 2) {
            return null
        }

        val name = elements[elements.size - 2]
        var version = elements[elements.size - 1]

        if (version.contains("-SNAPSHOT")) {
            val metadataResult = mavenFacade.findMetadata(repository, rootGav)
            val snapshot = if (metadataResult.isOk) metadataResult.get().versioning?.snapshot else null

            if (snapshot?.timestamp != null && snapshot.buildNumber != null) {
                version = "${version.replace("-SNAPSHOT", "")}-${snapshot.timestamp}-${snapshot.buildNumber}"
            }
        }

        return rootGav.resolve("${name}-${version}${JAVADOC_JAR_EXTENSION}")
    }

    private fun loadJavadocJarContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocContainer, ErrorResponse> {
        val container: JavadocContainer = createContainer(javadocFolder, repository, gav)

        if (Files.exists(container.javadocContainerIndex)) {
            return Result.ok(container)
        }

        val javadocUnpackPath = container.javadocUnpackPath

        Files.createDirectories(javadocUnpackPath)
        val copyJarPath = javadocUnpackPath.resolve(JAVADOC_JAR)

        return mavenFacade.findFile(LookupRequest(accessToken, repository.name, gav))
            .peek { (_, originInput) -> this.copyJavadocJar(originInput, copyJarPath) }
            .flatMap { this.unpackJavadocJar(copyJarPath, javadocUnpackPath) }
            .peek { this.createDocIndexHtml(container) }
            .map { container }
    }

    internal fun createContainer(javadocFolder: Path, repository: Repository, jarLocation: Location): JavadocContainer {
        val javadocContainerPath = javadocFolder
            .resolve(repository.name)
            .resolve(jarLocation.locationBeforeLast("/").toString())
            .resolve(CONTAINER_DIR)

        val javadocContainerIndex = javadocContainerPath.resolve(CONTAINER_VIEW_INDEX)
        val javadocUnpackPath = javadocContainerPath.resolve(CONTAINER_UNPACK_DIR)

        return JavadocContainer(javadocContainerIndex, javadocContainerPath, javadocUnpackPath)
    }

    private fun copyJavadocJar(originInput: InputStream, destinationJarPath: Path) {
        originInput.copyToAndClose(FileOutputStream(destinationJarPath.toString()))
    }

    private fun unpackJavadocJar(jarPath: Path, javadocUnpackPath: Path): Result<Unit, ErrorResponse> =
        when {
            !jarPath.getSimpleName().contains(JAVADOC_JAR) -> badRequestError("Invalid javadoc jar! Name must contain: '${JAVADOC_JAR}'")
            Files.isDirectory(jarPath) -> badRequestError("JavaDoc jar path has to be a file!")
            !Files.isDirectory(javadocUnpackPath) -> badRequestError("Destination must be a directory!")

            else -> jarPath.toAbsolutePath().toString()
                .let { JarFile(it) }
                .use { jarFile ->
                    if (!isIndexExist(jarFile)) {
                        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid javadoc.jar given for extraction")
                    }

                    jarFile.entries().asSequence().forEach { file ->
                        if (file.isDirectory) {
                            return@forEach
                        }

                        val path = Paths.get(javadocUnpackPath.toString() + "/" + file.name)

                        path.parent?.also { parent -> Files.createDirectories(parent) }
                        jarFile.getInputStream(file).copyToAndClose(path.outputStream())
                    }.asSuccess<Unit, ErrorResponse>()
                }
                .also { Files.deleteIfExists(jarPath) }
        }

    private fun createDocIndexHtml(container: JavadocContainer) {
        Files.write(container.javadocContainerIndex, JavadocView.index().toByteArray(Charsets.UTF_8))
    }

    private fun isJavadocJar(path: String) =
        path.endsWith(JAVADOC_JAR_EXTENSION)

    private fun isIndexExist(jarFile: JarFile) =
        jarFile.getJarEntry(JAVADOC_JAR_INDEX)?.isDirectory == false

    private fun InputStream.copyToAndClose(output: OutputStream) =
        this.use { output.use { this.copyTo(output) } }

}