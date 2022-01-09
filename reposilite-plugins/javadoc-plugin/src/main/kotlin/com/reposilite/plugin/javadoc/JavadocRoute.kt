package com.reposilite.plugin.javadoc

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.fs.FileType
import com.reposilite.shared.fs.getExtension
import com.reposilite.token.api.AccessToken
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes
import com.reposilite.web.http.ErrorResponse
import com.reposilite.web.http.encoding
import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import io.javalin.http.ContentType
import io.javalin.http.HttpCode
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Dec. 27, 2021
 */
class JavadocRoute(mavenFacade: MavenFacade, javadocFolder: File) : ReposiliteRoutes() {

    private val extractor: DocExtractor = DocExtractor()

    private val docRoute = ReposiliteRoute("/javadoc/{repository}/<gav>", RouteMethod.GET) {
        accessed {
            val repo = requiredParameter("repository")
            val gav = requiredParameter("gav")

            // TODO ability to download zipped javadoc via /javadoc/*/download

            val targetFolder = File(javadocFolder, "${repo}${File.separator}${gav}")
            if (targetFolder.exists() && (gav.endsWith(".html") || gav.endsWith(".css") || gav.endsWith(".js"))) {

                ctx.encoding(Charsets.UTF_8)
                ctx.contentType(ContentType.getMimeTypeByExtension(uri.getExtension()) ?: ContentType.PLAIN)

                try {
                    response = Files.readAllLines(targetFolder.toPath()).joinToString(separator = "\n")
                } catch (e: NoSuchFileException) {
                    response = ErrorResponse(HttpCode.NOT_FOUND, "Resource not found")
                    return@accessed
                }
                return@accessed
            }

            val newGav = constructGav(gav) // constructing new gav to count for non -javadoc.jar ending
            mavenFacade.findDetails(LookupRequest(this, repo, newGav)).consume(
                { file ->
                    if (file.type === FileType.FILE) {
                        if (!file.name.endsWith("-javadoc.jar")) {
                            response = ErrorResponse(
                                HttpCode.NOT_FOUND,
                                "Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!"
                            )
                            return@consume
                        }
                        val source = extractJavaDoc(newGav, repo, mavenFacade, javadocFolder, this)

                        ctx.encoding(Charsets.UTF_8)
                        ctx.contentType(ContentType.TEXT_HTML)
                        response = source
                        return@consume
                    }

                    // TODO handle latest
                    println("hi")
                    response = file.name
                }, { error ->
                    return@consume
                })
        }
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
    private fun extractJavaDoc(gav: String, repo: String, mavenFacade: MavenFacade, javadocFolder: File, token: AccessToken?): String {
        val path = gav.substringBeforeLast("/")
        val targetFolder = File(javadocFolder, "${repo}${File.separator}${path}")
        if (targetFolder.exists()) {
            return Files.readAllLines(File(targetFolder, "index.html").toPath()).joinToString(separator = "\n")
        }

        targetFolder.mkdirs()
        val targetJar = File(targetFolder, "doc.jar")

        mavenFacade.findFile(LookupRequest(token, repo, gav)).consume(
            { inputStream ->
                inputStream.use { inStream ->
                    FileOutputStream(targetJar).use {
                        inStream.copyTo(it)
                    }
                }

                extractor.extractJavadoc(targetJar.toPath(), targetFolder.toPath())
                targetJar.delete()
            },
            {
                return@consume
            }
        )

        File(targetFolder, "index.html").renameTo(File(targetFolder, "docindex.html"))
        writeNewIndex(targetFolder)

        return Files.readAllLines(File(targetFolder, "index.html").toPath()).joinToString(separator = "\n")
    }

    /**
     * Creates a new index.html file as a "holder" for the actual javadoc, so in the future we can have custom things embedded, like
     * switching between documents easily, downloading documents etc.
     *
     * WARNING/NOTE: this html contains an iframe which points to a docindex.html, that must be in the same directory as the index.html!
     *
     * @param targetFolder the folder in which the index file will be generated
     */
    fun writeNewIndex(targetFolder: File) {
        val index = File(targetFolder, "index.html")

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

        index.writeText(source, Charsets.UTF_8)
    }

    override val routes: Set<Route<ContextDsl, Unit>> = setOf(docRoute)
}