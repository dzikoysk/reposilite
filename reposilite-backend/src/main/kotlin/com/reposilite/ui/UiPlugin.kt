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

@Plugin(name = "ui")
internal class UiPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
        event { event: HttpServerConfigurationEvent ->
            event.config.registerPlugin(
                JtexPlugin {
                    it.mode = JtexConfig.Mode.RESOURCE
                    it.path = "ui"
                }
            )

            event.config.router.mount { routing: JavalinDefaultRouting ->
                routing
                    .get("/") {
                        it.render(
                            "index.jte",
                            mapOf("view" to IndexView(
                                title = "Reposilite",
                                description = "Reposilite is a lightweight repository management software dedicated to simplicity and speed.",
                                logo = "https://avatars.githubusercontent.com/u/88636591",
                                website = "https://reposilite.com"
                            ))
                        )
                    }
                    .post("/clicked") { ctx: Context -> ctx.result("Panda") }
            }
        }

        return null
    }

}