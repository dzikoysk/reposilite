package com.reposilite.dokka

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
import com.reposilite.storage.getSimpleName
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.outputStream
import panda.std.Result
import panda.std.asSuccess

private const val INDEX_FILE = "index.html"

internal class DokkaContainer(
    val dokkaContainerPath: Path,
    val dokkaContainerIndex: Path,
    val dokkaUnpackPath: Path
)

internal class DokkaContainerService(
    private val failureFacade: FailureFacade,
    private val mavenFacade: MavenFacade,
    private val dokkaFolder: Path
) {

    fun loadContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<DokkaContainer, ErrorResponse> {
        val dokkaJar = this.resolveDokkaJar(repository, gav) ?: return badRequestError("Invalid GAV")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository.name, dokkaJar))
            .filter({ it.type === FILE }, { badRequest("Invalid request") })
            .filter({ isDokkaJar(it.name) }, { notFound("Please do not provide a direct link to a non dokka file! GAV must be pointing to a directory or a dokka file!") })
            .flatMap { loadDokkaJarContainer(accessToken, repository, dokkaJar) }
    }

    private fun resolveDokkaJar(repository: Repository, gav: Location): Location? = when {
        gav.endsWith(".jar") -> gav
        gav.endsWith("/$INDEX_FILE") -> resolveIndexGav(repository, gav)
        else -> resolveDokkaJar(repository, gav.resolve(INDEX_FILE))
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

        return rootGav.resolve("${name}-${version}-dokka.jar")
    }

    private fun loadDokkaJarContainer(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<DokkaContainer, ErrorResponse> {
        val container: DokkaContainer = createContainer(dokkaFolder, repository, gav)

        if (Files.exists(container.dokkaContainerIndex)) {
            return Result.ok(container)
        }

        val dokkaUnpackPath = container.dokkaUnpackPath

        Files.createDirectories(dokkaUnpackPath)
        val copyJarPath = dokkaUnpackPath.resolve("dokka.jar")

        return mavenFacade.findFile(LookupRequest(accessToken, repository.name, gav))
            .peek { (_, originInput) -> this.copyDokkaJar(originInput, copyJarPath) }
            .flatMap { this.unpackDokkaJar(copyJarPath, dokkaUnpackPath) }
            .peek { this.createDocIndexHtml(container) }
            .map { container }
    }

    internal fun createContainer(dokkaFolder: Path, repository: Repository, jarLocation: Location): DokkaContainer {
        val dokkaContainerPath = dokkaFolder
            .resolve(repository.name)
            .resolve(jarLocation.locationBeforeLast("/").toString())
            .resolve(".cache")

        return DokkaContainer(
            dokkaContainerPath = dokkaContainerPath,
            dokkaContainerIndex = dokkaContainerPath.resolve(INDEX_FILE),
            dokkaUnpackPath = dokkaContainerPath.resolve("unpack")
        )
    }

    private fun copyDokkaJar(originInput: InputStream, destinationJarPath: Path) {
        originInput.copyToAndClose(FileOutputStream(destinationJarPath.toString()))
    }

    private fun unpackDokkaJar(jarPath: Path, dokkaUnpackPath: Path): Result<Unit, ErrorResponse> =
        when {
            !jarPath.getSimpleName().contains("dokka.jar") -> badRequestError("Invalid dokka jar! Name must contain: 'dokka.jar'")
            Files.isDirectory(jarPath) -> badRequestError("JavaDoc jar path has to be a file!")
            !Files.isDirectory(dokkaUnpackPath) -> badRequestError("Destination must be a directory!")
            else -> jarPath.toAbsolutePath().toString()
                .let { JarFile(it) }
                .use { jarFile ->
                    if (!hasIndex(jarFile)) {
                        return errorResponse(INTERNAL_SERVER_ERROR, "Invalid dokka.jar given for extraction")
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
                                .map { dokkaUnpackPath.resolve(it) }
                                .peek {
                                    it.parent?.also { parent -> Files.createDirectories(parent) }
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
                .also { Files.deleteIfExists(jarPath) }
        }

    private fun createDocIndexHtml(container: DokkaContainer) {
        Files.write(container.dokkaContainerIndex, DokkaView.index("/.cache/unpack/index.html").toByteArray(Charsets.UTF_8))
    }

    private fun isDokkaJar(path: String) =
        path.endsWith("-dokka.jar")

    private fun hasIndex(jarFile: JarFile) =
        jarFile.getJarEntry(INDEX_FILE)?.isDirectory == false

    private fun InputStream.copyToAndClose(output: OutputStream) =
        this.use { output.use { this.copyTo(output) } }

}