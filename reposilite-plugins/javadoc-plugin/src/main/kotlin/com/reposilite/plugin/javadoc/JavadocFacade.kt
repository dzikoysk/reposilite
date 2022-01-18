package com.reposilite.plugin.javadoc

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.javadoc.api.JavadocResponse
import com.reposilite.plugin.javadoc.api.ResolveJavadocRequest
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
import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.BAD_REQUEST
import org.intellij.lang.annotations.Language
import panda.std.Blank
import panda.std.Result
import panda.std.Result.ok
import panda.std.asSuccess
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.outputStream

class JavadocFacade internal constructor(
    private val javadocFolder: Path,
    private val mavenFacade: MavenFacade,
    private val journalist: Journalist
) : Journalist, Facade {

    fun resolveRequest(request: ResolveJavadocRequest): Result<JavadocResponse, ErrorResponse> {
        val (repository, gav, accessToken) = request

        if (gav.contains("/resources/fonts")) {
            return notFoundError("Fonts are not served!")
        }

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

        val javadocJar = resolveJavadocJar(gav) ?: return notFoundError("Invalid request")

        return mavenFacade.findDetails(LookupRequest(accessToken, repository, javadocJar))
            .filter({ it.type === FILE }, { ErrorResponse(BAD_REQUEST, "Invalid request") })
            .filter({ it.name.endsWith("-javadoc.jar") }, { notFound("Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!") })
            .flatMap { extractJavadocJar(javadocJar, repository, javadocFolder, accessToken) }
            .map { JavadocResponse(ContentType.HTML, it) }
    }

    /**
     * Constructs a new gav from the input gav, so we can handle paths that end with /index.html for example.
     * This method can be extended further, to support even more ways to get the same javadoc with less URL parameters.
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

        if (Files.exists(targetFolder)) {
            return ok(Files.readAllLines(targetFolder.resolve("index.html")).joinToString(separator = "\n"))
        }

        Files.createDirectories(targetFolder)
        val targetJar = targetFolder.resolve("doc.jar")

        return mavenFacade.findFile(LookupRequest(token, repository, gav))
            .peek {
                it.use {
                    FileOutputStream(targetJar.toString()).use { output ->
                        it.copyTo(output)
                    }
                }
            }
            .map { extractJavadoc(targetJar, targetFolder) }
            .map {
                Files.move(targetFolder.resolve("index.html"), targetFolder.resolve("docindex.html"))
                createIndex(targetFolder)
                Files.readAllLines(targetFolder.resolve("index.html")).joinToString(separator = "\n")
            }
    }

    /**
     * Creates a new index.html file as a "holder" for the actual javadoc, so in the future we can have custom things embedded, like
     * switching between documents easily, downloading documents etc.
     *
     * WARNING/NOTE: this html contains an iframe which points to a docindex.html, that must be in the same directory as the index.html!
     *
     * @param targetFolder the folder in which the index file will be generated
     */
    private fun createIndex(targetFolder: Path) {
        val index = targetFolder.resolve("index.html")

        @Language("html")
        val source = """
        <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <title>Reposilite - JavaDoc</title>
            </head>
            <style>
                :root {
                    --nav-height: 3rem;
                }

                body {
                    height: calc(100vh - 170px);
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-family: Arial, Helvetica, sans-serif;
                }
                    
                .sticky-nav {
                    position: fixed;

                    display: flex;
                    flex-direction: column;
                    justify-content: center;

                    top: 0;
                    left: 0;
                    width: calc(100vw - 2rem);
                    height: var(--nav-height);
                    padding-left: 1rem;
                    padding-right: 1rem;

                    background-color: #325064;
                    color: #FFFFFF;
                }

                .doc {
                    border-top: solid 3px #588DB0; 
                    position: fixed;
                    top: var(--nav-height);
                    left: 0;
                    width: 100%;
                    height: calc(100vh - var(--nav-height));
                }

                .row {
                    display: flex;
                    justify-content: flex-start;
                }

                a {
                    text-decoration: none;
                    color: white;
                    width: auto;
                    margin-right: 2rem;
                }

                .title {
                    margin-right: 2rem;
                }

                a:hover {
                    color: #e2dfdf;
                }
            </style>
            <body>
                <div class="sticky-nav">
                    <div class="row">
                        <a class="title" href="/"><h3>Reposilite</h3></a>
                        <!--<a href="#p"><h5>Download JavaDoc</h5></a> todo-->
                    </div>
                </div>
                <iframe id="javadoc" class="doc" src="docindex.html"></iframe>
                <script>
                if (!window.location.href.endsWith("/")) {
                    document.getElementById("javadoc").src = window.location.href + '/docindex.html'
                }
                </script>
            </body>
        </html>
        """.trimIndent()

        Files.writeString(index, source, Charsets.UTF_8)
    }

    private fun extractJavadoc(jarFilePath: Path, destination: Path): Result<Blank, ErrorResponse> {
        // Some checks, to make sure we're working with valid files/paths.
        if (Files.isDirectory(jarFilePath)) return errorResponse(BAD_REQUEST, "JavaDoc jar path has to be a file!")
        if (!Files.isDirectory(destination)) return errorResponse(BAD_REQUEST, "Destination must be a directory!")
        if (!jarFilePath.getSimpleName().contains("doc.jar")) return errorResponse(BAD_REQUEST, "Invalid javadoc jar! Name must contain: 'doc.jar'")

        return JarFile(jarFilePath.toAbsolutePath().toString()).use { jarFile ->
            if (jarFile.getEntry("index.html")?.isDirectory != false) { // Make sure we have an index.html file
                return errorResponse(HttpCode.INTERNAL_SERVER_ERROR, "Invalid doc.jar given for extraction!")
            }

            extractJavadoc(destination, jarFile).also {
                jarFile.close() // Since it may still use the file
                Files.deleteIfExists(jarFilePath)
            }
        }
    }

    private fun extractJavadoc(destination: Path, jarFile: JarFile): Result<Blank, ErrorResponse> {
        try {
            jarFile.entries().asSequence().forEach { file ->

                val javaFile = destination.resolve(file.name)
                if (file.isDirectory) {
                    Files.createDirectory(javaFile)
                } else {
                    jarFile.getInputStream(file).use { input ->
                        javaFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            return ok()
        } catch (exception: Exception) {
            return errorResponse(HttpCode.INTERNAL_SERVER_ERROR, exception.message.orEmpty())
        }
    }

    override fun getLogger(): Logger =
        journalist.logger

}