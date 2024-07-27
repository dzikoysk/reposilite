package com.reposilite.plugin.prometheus.metrics

import io.prometheus.metrics.core.metrics.GaugeWithCallback
import org.eclipse.jetty.util.thread.QueuedThreadPool
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit


object QueuedThreadPoolMetrics {
    fun register(queuedThreadPool: QueuedThreadPool) {
        GaugeWithCallback.builder()
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
            .register()

        GaugeWithCallback.builder()
            .name("jetty_queued_thread_pool_utilization")
            .help("Percentage of threads in use")
            .unit(MetricsUnit.RATIO)
            .callback { callback -> callback.call(queuedThreadPool.utilizationRate) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_queued_thread_pool_jobs")
            .help("Number of total jobs")
            .callback { callback -> callback.call(queuedThreadPool.queueSize.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_queued_thread_pool_low_on_threads")
            .help("Number of total jobs")
            .callback { callback -> callback.call(if (queuedThreadPool.isLowOnThreads) 1.0 else 0.0) }
            .register()
    }
}
