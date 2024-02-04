package com.reposilite.plugin.prometheus

import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.plugin.event
import com.reposilite.plugin.facade
import com.reposilite.plugin.prometheus.collectors.QueuedThreadPoolCollector
import com.reposilite.plugin.prometheus.collectors.StatisticsHandlerCollector
import com.reposilite.web.api.HttpServerConfigurationEvent
import com.reposilite.web.api.RoutingSetupEvent
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool

@Plugin(name = "prometheus", dependencies = ["failure"])
class PrometheusPlugin : ReposilitePlugin() {

    override fun initialize(): Facade? {
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
            failureFacade = facade(),
            prometheusUser = prometheusUser,
            prometheusPassword = prometheusPassword
        )

        event { event: HttpServerConfigurationEvent ->
            val server = event.config.pvt.jetty.server!!

            val statisticsHandler = StatisticsHandler()
            server.handler = statisticsHandler
            StatisticsHandlerCollector.initialize(statisticsHandler)
            logger.info("Prometheus | Default StatisticsHandler collector has been initialized.")

            when (val threadPool = server.threadPool) {
                is QueuedThreadPool -> {
                    QueuedThreadPoolCollector.initialize(threadPool)
                    logger.info("Prometheus | QueuedThreadPool collector has been initialized.")
                }
                else -> logger.info("Prometheus | Unsupported thread pool for collector: ${threadPool.javaClass.name}")
            }
        }

        event { event: RoutingSetupEvent ->
            event.registerRoutes(
                PrometheusEndpoints(
                    prometheusFacade = prometheusFacade,
                    prometheusPath = prometheusPath
                )
            )
        }

        return null
    }

}