package com.reposilite.plugin.javadoc

import com.reposilite.Reposilite
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.fs.FileType
import com.reposilite.shared.fs.getExtension
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
class JavadocRoute(mavenFacade: MavenFacade, javadocFolder: File, reposilite: Reposilite) : ReposiliteRoutes() {

    private val extractor: DocExtractor = DocExtractor()

    private val docRoute = ReposiliteRoute("/javadoc/<repo>/<path>", RouteMethod.GET) {
        val repo = ctx.pathParam("repo")
        val gav = ctx.pathParam("path").replace(repo + "/".toRegex(), "") // janky IK, maybe a better way to do it?


        if (gav.contains(".html") || gav.contains(".css") || gav.contains(".js")) {
            val targetFolder = File(javadocFolder, "${repo}${File.separator}${gav}")

            ctx.encoding(Charsets.UTF_8)
            ctx.contentType(ContentType.getMimeTypeByExtension(uri.getExtension()) ?: ContentType.PLAIN)
            try {
                response = Files.readAllLines(targetFolder.toPath()).joinToString(separator = "\n")
            } catch (e: NoSuchFileException) {
                response = ErrorResponse(HttpCode.NOT_FOUND, "Resource not found")
                return@ReposiliteRoute
            }
            return@ReposiliteRoute
        }

        mavenFacade.findDetails(LookupRequest(null, repo, gav)).consume(
            { file ->
                if (file.type === FileType.FILE) {
                    // TODO make, that the javadoc.jar file should not be passed as URL parameter instead like: /index.html .
                    //     Therefore construct gav
                    if (!file.name.contains("-javadoc.jar")) {
                        response = ErrorResponse(
                            HttpCode.NOT_FOUND,
                            "Please do not provide a direct link to a non javadoc file! GAV must be pointing to a directory or a javadoc file!"
                        )
                        return@consume
                    }
                    val source = extractJavaDoc(gav, repo, mavenFacade, javadocFolder, reposilite)

                    ctx.encoding(Charsets.UTF_8)
                    ctx.contentType(ContentType.TEXT_HTML)
                    response = source
                    return@consume
                }

                // TODO handle latest
                response = file.name
            }, { error ->
                return@consume
            })
    }

    private fun extractJavaDoc(gav: String, repo: String, mavenFacade: MavenFacade, javadocFolder: File, reposilite: Reposilite): String {
        val path = gav.substring(0, gav.lastIndexOf("/"))
        val targetFolder = File(javadocFolder, "${repo}${File.separator}${path}")
        if (targetFolder.exists()) {
            return Files.readAllLines(File(targetFolder, "index.html").toPath()).joinToString(separator = "\n")
        }

        targetFolder.mkdirs()
        val targetJar = File(targetFolder, "doc.jar")

        mavenFacade.findFile(LookupRequest(null, repo, gav)).consume(
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
        writeNewIndex(targetFolder, reposilite)
        return Files.readAllLines(File(targetFolder, "index.html").toPath()).joinToString(separator = "\n")
    }

    fun writeNewIndex(targetFolder: File, reposilite: Reposilite) {
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