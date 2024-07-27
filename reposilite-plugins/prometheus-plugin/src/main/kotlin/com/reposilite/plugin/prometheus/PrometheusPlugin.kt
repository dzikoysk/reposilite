package com.reposilite.plugin.prometheus

import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.ResolvedFileEvent
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.prometheus.metrics.JettyStatisticsHandlerMetrics
import com.reposilite.plugin.prometheus.metrics.QueuedThreadPoolMetrics
import com.reposilite.plugin.prometheus.metrics.ReposiliteMetrics
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.RoutingSetupEvent
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@Plugin(name = "prometheus", dependencies = ["failure", "statistics", "status"])
class PrometheusPlugin : ReposilitePlugin() {

    override fun initialize(): Facade {
        logger.info("")
        logger.info("--- Prometheus")

        val prometheusPath = System.getProperty("reposilite.prometheus.path")
            ?: System.getenv("REPOSILITE_PROMETHEUS_PATH")
            ?: "/metrics"

        val prometheusUser = System.getProperty("reposilite.prometheus.user")
            ?: System.getenv("REPOSILITE_PROMETHEUS_USER")
            ?: throw IllegalStateException("Prometheus user is not defined")

        val prometheusPassword = System.getProperty("reposilite.prometheus.password")
            ?: System.getenv("REPOSILITE_PROMETHEUS_PASSWORD")
            ?: throw IllegalStateException("Prometheus password is not defined")

        val prometheusFacade = PrometheusFacade(
            failureFacade = facade<FailureFacade>(),
            prometheusUser = prometheusUser,
            prometheusPassword = prometheusPassword
        )

        JvmMetrics.builder().register()
        logger.info("Prometheus | JVM metrics has been initialized.")

        val reposiliteMetrics = ReposiliteMetrics.builder {
            statisticsFacade = facade<StatisticsFacade>()
            statusFacade = facade<StatusFacade>()
            failureFacade = facade<FailureFacade>()
        }.register()

        event { event: ResolvedFileEvent ->
            event.result.map { (info, _) ->
                reposiliteMetrics.responseFileSizeSummary.observe(info.contentLength.toDouble())
                reposiliteMetrics.resolvedFileCounter.inc()
            }
        }

        event { _: DeployEvent ->
            reposiliteMetrics.mavenDeployCounter.inc()
        }

        logger.info("Prometheus | Reposilite metrics has been initialized")


        event { event: HttpServerConfigurationEvent ->
            val server = event.config.pvt.jetty.server!!
            val handler = StatisticsHandler()
            server.handler = handler

            val jettyMetrics = JettyStatisticsHandlerMetrics.builder {
                statisticsHandler = handler
            }.register()

            // Abusing request log, as it's the only way to get the time the request was sent
            server.setRequestLog { request, response ->
                jettyMetrics.responseTimeSummary.labelValues(response.status.toString())
                    .observe((System.currentTimeMillis() - request.timeStamp).milliseconds.toDouble(DurationUnit.SECONDS))

                jettyMetrics.responseSizeSummary.observe(response.contentCount.toDouble())
            }

            event.config.router.mount {
                it.after { ctx ->
                    jettyMetrics.responseCounter.labelValues(ctx.statusCode().toString()).inc()
                }
            }

            logger.info("Prometheus | Jetty StatisticsHandler metrics has been initialized.")


            when (val threadPool = server.threadPool) {
                is QueuedThreadPool -> {
                    QueuedThreadPoolMetrics.builder {
                        queuedThreadPool = threadPool
                    }.register()
                    logger.info("Prometheus | Queued Thread Pool metrics has been initialized.")
                }

                else -> logger.warn("Prometheus | Unsupported thread pool for metrics: ${threadPool.javaClass.name}, ignoring")
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(PrometheusEndpoints(prometheusFacade = prometheusFacade, prometheusPath = prometheusPath))
        }

        return prometheusFacade
    }

}
