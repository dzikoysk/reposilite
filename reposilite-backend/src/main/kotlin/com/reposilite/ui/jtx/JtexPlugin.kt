package com.reposilite.ui.jtx

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import gg.jte.resolve.ResourceCodeResolver
import gg.jte.watcher.DirectoryWatcher
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Paths
import java.util.function.Consumer

class JtexConfig {

    enum class Mode {
        RESOURCE,
        DIRECTORY,
    }

    @JvmField var mode: Mode = Mode.RESOURCE
    @JvmField var path: String = "src/main/resources"
    @JvmField var sseRefreshEndpoint: String = "/refreshDevMode"
}

class JtexPlugin(userConfig: Consumer<JtexConfig>) : Plugin<JtexConfig>(userConfig = userConfig, defaultConfig = JtexConfig()) {

    override fun onStart(config: JavalinConfig) {
        val codeResolver = when(pluginConfig.mode) {
            JtexConfig.Mode.RESOURCE -> ResourceCodeResolver(pluginConfig.path)
            JtexConfig.Mode.DIRECTORY -> JtexDictionaryCodeResolver(Paths.get(pluginConfig.path).toAbsolutePath().normalize())
        }
        Paths.get("jte-classes")
        val templatingEngine = TemplateEngine.create(codeResolver, Paths.get(".build"), ContentType.Html)
        templatingEngine.setProjectNamespace(".compiled")
        config.fileRenderer(JavalinJte(templatingEngine))

        config.router.mount {
            it.sse(pluginConfig.sseRefreshEndpoint) { sseClient ->
                if (codeResolver is DirectoryCodeResolver) {
                    val watcher = DirectoryWatcher(templatingEngine, codeResolver)

                    watcher.start { templates ->
                        System.out.println("[watcher] Template changed, available at http://localhost:${config.jetty.defaultPort}")
                        sseClient.sendEvent("refresh", "Template changed, refresh browser event")
                    }

                    sseClient.onClose(watcher::stop)
                    sseClient.keepAlive()
                }
            }
        }
    }

}