package com.reposilite.ui

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.ui.jtx.JtexConfig
import com.reposilite.ui.jtx.JtexPlugin
import com.reposilite.ui.views.IndexView
import com.reposilite.web.api.HttpServerConfigurationEvent
import io.javalin.http.Context
import io.javalin.router.JavalinDefaultRouting

@Plugin(name = "reposilite-test/workspace/ui")
internal class UiPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        event { event: HttpServerConfigurationEvent ->
            event.config.registerPlugin(
                JtexPlugin {
                    it.mode = JtexConfig.Mode.DIRECTORY
                    it.path = "ui"
                }
            )

            val view =
                IndexView(
                    title = "Reposilite Repository",
                    description = "Official public Maven repository powered by Reposilite",
                    logo = "logo.png",
                    website = "https://reposilite.com",
                    repositories = listOf(
                        IndexView.Repository("assets", "Generic"),
                        IndexView.Repository("releases", "Maven"),
                        IndexView.Repository("snapshots", "Maven"),
                        IndexView.Repository("packages", "NPM"),
                        IndexView.Repository("registry", "Docker"),
                    ),
                    highlightedProjects = listOf(
                        IndexView.Project(
                            name = "Reposilite",
                            description = "Reposilite is a lightweight repository management software dedicated to simplicity and speed.",
                            routes = listOf(
                                IndexView.Route(IndexView.Repository("releases", "Maven"), "/com/reposilite/reposilite-backend"),
                                IndexView.Route(IndexView.Repository("snapshots", "Maven"), "/com/reposilite/reposilite-backend"),
                                IndexView.Route(IndexView.Repository("registry", "Docker"), "/dzikoysk/reposilite")
                            )
                        ),
                        IndexView.Project(
                            name = "Javalin",
                            description = "A simple web framework for Java and Kotlin.",
                            routes = listOf(
                                IndexView.Route(IndexView.Repository("releases", "Maven"), "/io/javalin/javalin"),
                                IndexView.Route(IndexView.Repository("snapshots", "Maven"), "/io/javalin/javalin"),
                            )
                        ),
                        IndexView.Project(
                            name = "Khangul",
                            description = "Hangul processor for Kotlin Multiplatform & JavaScript projects, based on reverse-engineered enhanced version of Branah keyboard algorithm \uD83C\uDDF0\uD83C\uDDF7",
                            routes = listOf(
                                IndexView.Route(IndexView.Repository("packages", "NPM"), "@dzikoysk/khangul")
                            )
                        )
                    )
                )

            event.config.router.mount { routing: JavalinDefaultRouting ->
                routing
                    .get("/") {
                        it.render(
                            "index.jte",
                            mapOf("view" to view)
                        )
                    }
                    .get("/releases") {
                        it.render(
                            "index.jte",
                            mapOf("view" to view.copy(
                                browsedRepository = IndexView.BrowsedRepository(
                                    name = "releases",
                                    files = listOf(
                                        IndexView.BrowsedRepository.Directory("assets"),
                                        IndexView.BrowsedRepository.Directory("com"),
                                        IndexView.BrowsedRepository.Directory("io"),
                                        IndexView.BrowsedRepository.Directory("org"),
                                        IndexView.BrowsedRepository.File("index.html", "1.2 KB"),
                                        IndexView.BrowsedRepository.File("index.js", "2.3 KB"),
                                        IndexView.BrowsedRepository.File("index.css", "0.5 KB"),
                                    )
                                )
                            ))
                        )
                    }
                    .post("/clicked") { ctx: Context -> ctx.result("Panda") }
            }
        }

        return null
    }

}