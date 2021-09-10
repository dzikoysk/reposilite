package com.reposilite.web.infrastructure

import com.reposilite.Reposilite
import com.reposilite.ReposiliteWebConfiguration
import com.reposilite.config.Configuration
import com.reposilite.shared.TimeUtils
import com.reposilite.web.ReposiliteContextFactory
import com.reposilite.web.ReposiliteWebDsl
import com.reposilite.web.WebServer
import com.reposilite.web.http.response
import com.reposilite.web.routing.ReactiveRoutingPlugin
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.util.JavalinLogger
import io.javalin.http.Context
import io.javalin.jetty.JettyUtil
import kotlinx.coroutines.CoroutineDispatcher
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.util.thread.ThreadPool

internal class JavalinWebServer(private val threadPool: ThreadPool) : WebServer {

    private val servlet = false
    private var javalin: Javalin? = null

    override fun start(reposilite: Reposilite) {
        val configuration = reposilite.configuration
        val dispatcher = reposilite.dispatcher

        JavalinLogger.enabled = false
        JettyUtil.disableJettyLogger()

        this.javalin = createJavalin(reposilite, configuration, threadPool, dispatcher)
            .exception(EofException::class.java, { exception, ctx -> reposilite.logger.warn("Client closed connection") })
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

        JavalinLogger.enabled = true
        JettyUtil.reEnableJettyLogger()
    }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration, threadPool: ThreadPool, dispatcher: CoroutineDispatcher): Javalin =
        if (servlet) {
            Javalin.createStandalone { configureServer(reposilite, configuration, threadPool, dispatcher, it) }
        } else {
            Javalin.create { configureServer(reposilite, configuration, threadPool, dispatcher, it) }
        }

    private fun configureServer(reposilite: Reposilite, configuration: Configuration, threadPool: ThreadPool, dispatcher: CoroutineDispatcher, serverConfig: JavalinConfig) {
        JavalinWebServerConfiguration.configure(reposilite, threadPool, configuration, serverConfig)

        val plugin = ReactiveRoutingPlugin<ReposiliteWebDsl, Unit>(
            errorConsumer = { name, error -> reposilite.logger.error("Coroutine $name failed to execute task", error) },
            dispatcher = dispatcher,
            syncHandler = { ctx, route ->
                val resultDsl = callWithContext(reposilite.contextFactory, ctx, route.handler)
                resultDsl.response?.also { ctx.response(it) }
            },
            asyncHandler = { ctx, route, result ->
                try {
                    val dsl = callWithContext(reposilite.contextFactory, ctx, route.handler)
                    dsl.response?.also { ctx.response(it) }
                    result.complete(Unit)
                }
                catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    result.completeExceptionally(throwable)
                }
            }
        )

        ReposiliteWebConfiguration.routing(reposilite).forEach { plugin.registerRoutes(it) }
        serverConfig.registerPlugin(plugin)
    }

    private suspend fun callWithContext(contextFactory: ReposiliteContextFactory, ctx: Context, consumer: suspend ReposiliteWebDsl.() -> Unit): ReposiliteWebDsl =
        ReposiliteWebDsl(ctx, contextFactory.create(ctx)).also { consumer(it) }

    override fun stop() {
        javalin?.stop()
    }

    override fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}