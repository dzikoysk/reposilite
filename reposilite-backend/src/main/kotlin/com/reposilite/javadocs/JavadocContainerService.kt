package com.reposilite.javadocs

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.shared.badRequestError
import com.reposilite.shared.errorResponse
import com.reposilite.shared.notFound
import com.reposilite.status.FailureFacade
import com.reposilite.storage.api.FileType.FILE
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import panda.std.Result
import panda.std.asSuccess
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

private const val INDEX_FILE = "index.html"

internal class JavadocContainer(
    val javadocContainerPath: Path,
    val javadocContainerIndex: Path,
    val javadocUnpackPath: Path
)

internal class JavadocContainerService(
    private val failureFacade: FailureFacade,
    private val mavenFacade: MavenFacade,
    private val javadocFolder: Path
) {

    fun loadContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocContainer, ErrorResponse> {
        val javadocJar = this.resolveJavadocJar(repository, gav) ?: return badRequestError("Invalid GAV")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository.name, javadocJar))
            .filter({ it.type === FILE }, { badRequest("Invalid request") })
            .filter({ isJavadocJar(it.name) }, { notFound("Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!") })
            .flatMap { loadJavadocJarContainer(accessToken, repository, javadocJar) }
    }

    private fun resolveJavadocJar(repository: Repository, gav: Location): Location? = when {
        gav.getExtension() == "jar" -> gav
        gav.endsWith("/$INDEX_FILE") -> resolveIndexGav(repository, gav)
        else -> resolveJavadocJar(repository, gav.resolve(INDEX_FILE))
    }

    private fun resolveIndexGav(repository: Repository, gav: Location): Location? {
        val rootGav = gav.locationBeforeLast("/$INDEX_FILE")
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

        return rootGav.resolve("${name}-${version}-javadoc.jar")
    }

    private fun loadJavadocJarContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocContainer, ErrorResponse> {
        val container: JavadocContainer = createContainer(javadocFolder, repository, gav)

        if (container.javadocContainerIndex.exists()) {
            return Result.ok(container)
        }

        val javadocUnpackPath = container.javadocUnpackPath

        javadocUnpackPath.createDirectories()
        val copyJarPath = javadocUnpackPath.resolve("javadoc.jar")

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
            .resolve(".cache")

        return JavadocContainer(
            javadocContainerPath = javadocContainerPath,
            javadocContainerIndex = javadocContainerPath.resolve(INDEX_FILE),
            javadocUnpackPath = javadocContainerPath.resolve("unpack")
        )
    }

    private fun copyJavadocJar(originInput: InputStream, destinationJarPath: Path) {
        originInput.copyToAndClose(FileOutputStream(destinationJarPath.toString()))
    }

    private fun unpackJavadocJar(jarPath: Path, javadocUnpackPath: Path): Result<Unit, ErrorResponse> =
        when {
            !jarPath.name.contains("javadoc.jar") -> badRequestError("Invalid javadoc jar! Name must contain: 'javadoc.jar'")
            jarPath.isDirectory() -> badRequestError("JavaDoc jar path has to be a file!")
            !javadocUnpackPath.isDirectory() -> badRequestError("Destination must be a directory!")
            else -> jarPath.toAbsolutePath().toString()
                .let { JarFile(it) }
                .use { jarFile ->
                    if (!hasIndex(jarFile)) {
                        return errorResponse(INTERNAL_SERVER_ERROR, "Invalid javadoc.jar given for extraction")
                    }

                    jarFile
                        .entries()
                        .asSequence()
                        .forEach { file ->
                            if (file.isDirectory) {
                                return@forEach
                            }

                            // GHSA-frvj-cfq4-3228: treat archive file name as external path that can be malicious
                            val processedArchiveFileLocation = file.name.toLocation()

                            processedArchiveFileLocation
                                .toPath()
                                .map { javadocUnpackPath.resolve(it) }
                                .peek {
                                    it.createParentDirectories()
                                    jarFile.getInputStream(file).copyToAndClose(it.outputStream())
                                }
                                .onError {
                                    failureFacade.throwException(
                                        "Malicious resource path detected: $processedArchiveFileLocation in $jarPath",
                                        IllegalArgumentException("Malicious resource path detected: $it")
                                    )
                                }
                        }
                        .asSuccess<Unit, ErrorResponse>()
                }
                .also { if (jarPath.exists()) jarPath.deleteExisting() }
        }

    private fun createDocIndexHtml(container: JavadocContainer) {
        container.javadocContainerIndex.writeText(JavadocView.index("/.cache/unpack/index.html"))
    }

    private fun isJavadocJar(path: String) =
        path.endsWith("-javadoc.jar")

    private fun hasIndex(jarFile: JarFile) =
        jarFile.getJarEntry(INDEX_FILE)?.isDirectory == false

    private fun InputStream.copyToAndClose(output: OutputStream) =
        this.use { output.use { this.copyTo(output) } }

}