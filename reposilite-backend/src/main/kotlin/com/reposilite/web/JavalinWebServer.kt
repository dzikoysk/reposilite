package com.reposilite.web

import com.reposilite.Reposilite
import com.reposilite.ReposiliteWebConfiguration
import com.reposilite.config.Configuration
import com.reposilite.shared.TimeUtils
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import kotlinx.coroutines.CoroutineDispatcher
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool

class JavalinWebServer {

    private val servlet = false
    private var javalin: Javalin? = null
    private var webThreadPool: QueuedThreadPool? = null

    fun start(reposilite: Reposilite) =
        runWithDisabledLogging {
            this.webThreadPool = QueuedThreadPool(reposilite.configuration.webThreadPool, 2)
            webThreadPool?.start()

            this.javalin = createJavalin(reposilite, reposilite.configuration, webThreadPool!!, reposilite.ioDispatcher)
                .exception(EofException::class.java, { _, _ -> reposilite.logger.warn("Client closed connection") })
                .events { listener ->
                    listener.serverStopping { reposilite.logger.info("Server stopping...") }
                    listener.serverStopped { reposilite.logger.info("Bye! Uptime: " + TimeUtils.getPrettyUptimeInMinutes(reposilite.startTime)) }
                }
                .also {
                    ReposiliteWebConfiguration.javalin(reposilite, it)
                }

            if (!servlet) {
                javalin!!.start(reposilite.parameters.hostname, reposilite.parameters.port)
            }
        }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration, threadPool: ThreadPool, dispatcher: CoroutineDispatcher): Javalin =
        if (servlet)
            Javalin.createStandalone { configureServer(reposilite, configuration, threadPool, dispatcher, it) }
        else
            Javalin.create { configureServer(reposilite, configuration, threadPool, dispatcher, it) }

    private fun configureServer(reposilite: Reposilite, configuration: Configuration, threadPool: ThreadPool, dispatcher: CoroutineDispatcher, serverConfig: JavalinConfig) {
        WebServerConfiguration.configure(reposilite, threadPool, configuration, serverConfig)
        serverConfig.registerPlugin(createReactiveRouting(reposilite))
    }

    fun stop() {
        webThreadPool?.stop()
        javalin?.stop()
    }

    fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}