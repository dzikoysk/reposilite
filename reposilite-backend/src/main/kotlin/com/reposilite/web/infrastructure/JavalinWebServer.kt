package com.reposilite.web.infrastructure

import com.reposilite.Reposilite
import com.reposilite.ReposiliteWebConfiguration
import com.reposilite.config.Configuration
import com.reposilite.web.WebServer
import com.reposilite.shared.alsoIf
import io.javalin.Javalin

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
            .also {
                ReposiliteWebConfiguration.javalin(reposilite, it)
                JavalinWebServerConfiguration.routing(reposilite, it)
            }
            .alsoIf(!servlet) {
                it.start(configuration.hostname, configuration.port)
            }
    }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration): Javalin =
        if (servlet)
            Javalin.createStandalone { JavalinWebServerConfiguration.configure(reposilite, configuration, it) }
        else
            Javalin.create { JavalinWebServerConfiguration.configure(reposilite, configuration, it) }

    override fun stop() {
        javalin?.stop()
    }

    override fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}