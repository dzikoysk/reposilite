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
import io.javalin.http.Context
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

internal class JavalinWebServer : WebServer {

    private val servlet = false
    private var javalin: Javalin? = null

    override fun start(reposilite: Reposilite) {
        val configuration = reposilite.configuration
        val dispatcher = DispatcherWithShutdown(reposilite.coreThreadPool.asCoroutineDispatcher())

        this.javalin = createJavalin(reposilite, configuration, dispatcher)
            .events { listener ->
                listener.serverStopping {
                    reposilite.logger.info("Server stopping...")
                    dispatcher.prepareShutdown()
                }
                listener.serverStopped {
                    dispatcher.completeShutdown()
                    reposilite.coreThreadPool.stop()
                    reposilite.logger.info("Bye! Uptime: " + TimeUtils.getPrettyUptimeInMinutes(reposilite.startTime))
                }
            }
            .also {
                ReposiliteWebConfiguration.javalin(reposilite, it)
                reposilite.coreThreadPool.start()
            }

        if (!servlet) {
            javalin!!.start(reposilite.parameters.hostname, reposilite.parameters.port)
        }
    }

    private fun createJavalin(reposilite: Reposilite, configuration: Configuration, dispatcher: CoroutineDispatcher): Javalin =
        if (servlet) {
            Javalin.createStandalone { configureServer(reposilite, configuration, dispatcher, it) }
        } else {
            Javalin.create { configureServer(reposilite, configuration, dispatcher, it) }
        }

    private fun configureServer(reposilite: Reposilite, configuration: Configuration, dispatcher: CoroutineDispatcher, serverConfig: JavalinConfig) {
        JavalinWebServerConfiguration.configure(reposilite, configuration, serverConfig)

        val plugin = ReactiveRoutingPlugin<ReposiliteWebDsl, Unit>(
            errorConsumer = { name, error -> reposilite.logger.error("Coroutine $name failed to execute task", error) },
            dispatcher = dispatcher,
            syncHandler = { ctx, route ->
                val resultDsl = callWithContext(reposilite.contextFactory, ctx, route.handler)
                resultDsl.response?.also { ctx.response(it) }
            },
            asyncHandler = { ctx, route, result ->
                val resultDsl = callWithContext(reposilite.contextFactory, ctx, route.handler)
                resultDsl.response?.also { ctx.response(it) }
                result.complete(Unit)
            }
        )

        ReposiliteWebConfiguration.routing(reposilite).forEach { plugin.registerRoutes(it) }
        serverConfig.registerPlugin(plugin)
    }

    suspend fun callWithContext(contextFactory: ReposiliteContextFactory, ctx: Context, init: suspend ReposiliteWebDsl.() -> Unit): ReposiliteWebDsl =
        ReposiliteWebDsl(ctx, contextFactory.create(ctx))
            .also { init(it) }

    override fun stop() {
        javalin?.stop()
    }

    override fun isAlive(): Boolean =
        javalin?.jettyServer()?.server()?.isStarted ?: false

}