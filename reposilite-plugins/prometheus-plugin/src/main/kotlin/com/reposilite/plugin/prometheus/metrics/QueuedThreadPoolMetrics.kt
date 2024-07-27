package com.reposilite.plugin.prometheus.metrics

import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.eclipse.jetty.util.thread.QueuedThreadPool
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit


class QueuedThreadPoolMetrics(
    private val config: PrometheusProperties,
    private val queuedThreadPool: QueuedThreadPool
) {

    private fun register(registry: PrometheusRegistry) {
        GaugeWithCallback.builder(config)
            .name("jetty_queued_thread_pool_threads_state")
            .help("Number of threads by state")
            .labelNames("state")
            .callback { callback ->
                callback.call(queuedThreadPool.threads.toDouble(), "total")
                callback.call(queuedThreadPool.maxThreads.toDouble(), "max")
                callback.call(queuedThreadPool.minThreads.toDouble(), "min")
                callback.call(queuedThreadPool.readyThreads.toDouble(), "ready")
                callback.call(queuedThreadPool.idleThreads.toDouble(), "idle")
                callback.call(queuedThreadPool.busyThreads.toDouble(), "busy")
                callback.call(queuedThreadPool.utilizedThreads.toDouble(), "utilized")
                callback.call(queuedThreadPool.maxAvailableThreads.toDouble(), "max_available")
            }
            .register(registry)

        GaugeWithCallback.builder(config)
            .name("jetty_queued_thread_pool_utilization")
            .help("Percentage of threads in use")
            .unit(MetricsUnit.RATIO)
            .callback { callback -> callback.call(queuedThreadPool.utilizationRate) }
            .register(registry)

        GaugeWithCallback.builder(config)
            .name("jetty_queued_thread_pool_jobs")
            .help("Number of total jobs")
            .callback { callback -> callback.call(queuedThreadPool.queueSize.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder(config)
            .name("jetty_queued_thread_pool_low_on_threads")
            .help("Number of total jobs")
            .callback { callback -> callback.call(if (queuedThreadPool.isLowOnThreads) 1.0 else 0.0) }
            .register(registry)
    }

    class Builder(private val config: PrometheusProperties) {
        var queuedThreadPool: QueuedThreadPool? = null

        fun register(registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry) =
            QueuedThreadPoolMetrics(config, queuedThreadPool!!).apply {
                register(registry)
            }
    }

    companion object {
        fun builder(build: Builder.() -> Unit) = builder(PrometheusProperties.get(), build)

        fun builder(config: PrometheusProperties, build: Builder.() -> Unit) = Builder(config).also { it.build() }
    }
}
