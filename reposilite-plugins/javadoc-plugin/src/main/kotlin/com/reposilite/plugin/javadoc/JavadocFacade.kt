package com.reposilite.plugin.javadoc

import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.fs.FileType
import com.reposilite.token.api.AccessToken
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.errorResponse
import io.javalin.http.ContentType
import io.javalin.http.HttpCode
import org.intellij.lang.annotations.Language
import panda.std.Result
import panda.std.Result.ok
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

class JavadocFacade internal constructor(
    private val javadocFolder: Path,
    private val mavenFacade: MavenFacade,
    private val journalist: Journalist
) : Journalist, Facade {

    private val extractor = DocExtractor()

    fun resolveRequest(repo: String, gav: String, extension: String, accessToken: AccessToken?): Result<JavadocResponse, ErrorResponse> {
        val target = javadocFolder.resolve("${repo}${File.separator}${gav}")
        if (gav.contains("/resources/fonts")) return errorResponse(HttpCode.NOT_FOUND, "Fonts are not served!")

        if (Files.exists(target) && (gav.endsWith(".html") || gav.endsWith(".css") || gav.endsWith(".js"))) {
            return try {
                val contentType = ContentType.getMimeTypeByExtension(extension) ?: ContentType.PLAIN
                val response = Files.readAllLines(target).joinToString(separator = "\n")

                ok(JavadocResponse(contentType, response))
            } catch (e: NoSuchFileException) {
                errorResponse(HttpCode.NOT_FOUND, "Resource not found!")
            }
        }

        val newGav = constructGav(gav)
        val lookUp = mavenFacade.findDetails(LookupRequest(accessToken, repo, newGav));

        if (lookUp.isOk) {
            val file = lookUp.get()
            if (file.type === FileType.FILE) {
                if (!file.name.endsWith("-javadoc.jar")) {
                    return  errorResponse(
                        HttpCode.NOT_FOUND,
                        "Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!"
                    )
                }

                val source = extractJavaDoc(newGav, repo, mavenFacade, javadocFolder, accessToken)
                return ok(JavadocResponse(ContentType.HTML, source))
            }

            //TODO handle latest
            return errorResponse(HttpCode.BAD_REQUEST, "Invalid request!")
        }
        return error(lookUp.error)
    }

    /**
     * Constructs a new gav from the input gav, so we can handle paths that end with /index.html for example.
     * This method can be extended further, to support even more ways to get the same javadoc with less URL parameters.
     */
    private fun constructGav(gav: String): String {
        if (gav.endsWith("index.html")) {
            val path = gav.substringBeforeLast("/index.html")
            val split = path.split("/".toRegex())

            val version = split[split.size - 1]
            val name = split[split.size - 2]

            return "${path}/${name}-${version}-javadoc.jar"
        } else if (gav.endsWith(".jar")) {
            return gav //original gav
        }

        val append = if (gav.endsWith("/")) "" else "/"
        return constructGav(gav + append + "index.html")
    }

    /**
     * Retrieves the javadoc jar file from mavenFacade using a LookupRequest.
     * Then it will extract the files from the jar file, rename index.html to docindex.html and generate a new index.html file using the writeNewIndex method.
     *
     * @param gav the direct gav to the javadoc file
     * @param mavenFacade the MavenFacade to use
     * @param javadocFolder the target folder in which the files should be extracted
     */
    private fun extractJavaDoc(gav: String, repo: String, mavenFacade: MavenFacade, javadocFolder: Path, token: AccessToken?): Result<String, ErrorResponse> {
        val path = gav.substringBeforeLast("/")
        val targetFolder = javadocFolder.resolve("${repo}${File.separator}${path}")
        if (Files.exists(targetFolder)) {
            return ok(Files.readAllLines(targetFolder.resolve("index.html")).joinToString(separator = "\n"))
        }

        Files.createDirectories(targetFolder)
        val targetJar = targetFolder.resolve("doc.jar")

        val findResult = mavenFacade.findFile(LookupRequest(token, repo, gav))
        if (findResult.isOk) {
            val inputStream = findResult.get()
            inputStream.use { inStream ->
                FileOutputStream(targetJar.toString()).use {
                    inStream.copyTo(it)
                }
            }

            val result = extractor.extractJavadoc(targetJar, targetFolder)

            if (result.isOk) {
                Files.move(targetFolder.resolve("index.html"), targetFolder.resolve("docindex.html"))
                writeNewIndex(targetFolder)

                return ok(Files.readAllLines(targetFolder.resolve("index.html")).joinToString(separator = "\n"))
            }

            return error(result.error)
        }
        return error(findResult.error)
    }

    /**
     * Creates a new index.html file as a "holder" for the actual javadoc, so in the future we can have custom things embedded, like
     * switching between documents easily, downloading documents etc.
     *
     * WARNING/NOTE: this html contains an iframe which points to a docindex.html, that must be in the same directory as the index.html!
     *
     * @param targetFolder the folder in which the index file will be generated
     */
    private fun writeNewIndex(targetFolder: Path) {
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
                <iframe class="doc" src="docindex.html"></iframe>
            </body>
        </html>
        """.trimIndent()

        Files.writeString(index, source, Charsets.UTF_8)
    }

    override fun getLogger(): Logger =
        journalist.logger
}

