package com.reposilite.plugin.prometheus.metrics

import com.reposilite.statistics.StatisticsFacade
import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.CounterWithCallback
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.core.metrics.Summary
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlin.Unit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit

class ReposiliteMetrics(
    private val config: PrometheusProperties,
    private val statisticsHandler: StatisticsFacade,
    private val statusFacade: StatusFacade,
    private val failureFacade: FailureFacade,
) {
    lateinit var responseTimeSummary: Summary
    lateinit var responseFileSizeSummary: Summary
    lateinit var responseCounter: Counter
    lateinit var mavenDeployCounter: Counter

    private fun register(registry: PrometheusRegistry) {
        GaugeWithCallback.builder(config)
            .name("reposilite_uptime_seconds")
            .help("Uptime")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statusFacade.fetchInstanceStatus().uptime.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register(registry)

        CounterWithCallback.builder(config)
            .name("reposilite_failures_total")
            .help("Number of failures reposilite has encountered")
            .callback { callback -> callback.call(failureFacade.getFailures().size.toDouble()) }
            .register(registry)

        responseFileSizeSummary = Summary.builder(config)
            .name("reposilite_response_file_size_bytes")
            .help("Size in bytes of responses")
            .unit(MetricsUnit.BYTES)
            .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
            .register(registry)

        responseTimeSummary = Summary.builder(config)
            .name("reposilite_response_time_seconds")
            .help("Size in bytes of responses")
            .unit(MetricsUnit.BYTES)
            .labelNames("code")
            .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
            .register(registry)

        responseCounter = Counter.builder(config)
            .name("reposilite_response_total")
            .help("Total response count")
            .labelNames("code")
            .register(registry)

        mavenDeployCounter = Counter.builder(config)
            .name("reposilite_deploy_total")
            .help("Total successful deployments count")
            .register(registry)
    }

    class Builder(private val config: PrometheusProperties) {
        var statisticsFacade: StatisticsFacade? = null
        var statusFacade: StatusFacade? = null
        var failureFacade: FailureFacade? = null

        fun register(registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry) =
            ReposiliteMetrics(config, statisticsFacade!!, statusFacade!!, failureFacade!!).apply {
                register(registry)
            }
    }

    companion object {
        fun builder(build: Builder.() -> Unit) = builder(PrometheusProperties.get(), build)

        fun builder(config: PrometheusProperties, build: Builder.() -> Unit) = Builder(config).apply(build)
    }

    fun Summary.Builder.quantile(vararg quantiles: Double): Summary.Builder {
        for (quantile in quantiles) {
            this.quantile(quantile)
        }
        return this
    }
}
