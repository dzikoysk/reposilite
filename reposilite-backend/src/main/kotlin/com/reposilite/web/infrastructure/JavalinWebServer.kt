package com.reposilite.web.infrastructure

import com.reposilite.Reposilite
import com.reposilite.ReposiliteWebConfiguration
import com.reposilite.config.Configuration
import com.reposilite.web.DslContext
import com.reposilite.web.WebServer
import com.reposilite.web.routing.RoutingPlugin
import io.javalin.Javalin
import io.javalin.core.JavalinConfig

internal class JavalinWebServer : WebServer {

    private val servlet = false
    private var javalin: Javalin? = null

    override fun start(reposilite: Reposilite) {
        val configuration = reposilite.configuration

        this.javalin = createJavalin(reposilite, configuration)
            .events { listener ->
                listener.serverStopping { reposilite.logger.info("Server stopping...") }
                listener.serverStopped { reposilite.logger.info("Bye! Uptime: " + reposilite.getPrettyUptime()) }
            }
            .also { ReposiliteWebConfiguration.javalin(reposilite, it) }

        if (!servlet) {
            javalin!!.start(configuration.hostname, configuration.port)
        }
    }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration): Javalin =
        if (servlet)
            Javalin.createStandalone { configureServer(reposilite, configuration, it) }
        else
            Javalin.create { configureServer(reposilite, configuration, it) }

    private fun configureServer(reposilite: Reposilite, configuration: Configuration, serverConfig: JavalinConfig) {
        JavalinWebServerConfiguration.configure(reposilite, configuration, serverConfig)

        RoutingPlugin { DslContext(it, reposilite.contextFactory.create(it)) }
            .also { plugin ->
                ReposiliteWebConfiguration.routing(reposilite).forEach {
                    plugin.registerRoutes(it)
                }
            }
            .also { serverConfig.registerPlugin(it) }
    }

    override fun stop() {
        javalin?.stop()
    }

    override fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}