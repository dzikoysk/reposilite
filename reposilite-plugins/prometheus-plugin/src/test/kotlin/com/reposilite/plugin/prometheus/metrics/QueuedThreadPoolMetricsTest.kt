package com.reposilite.plugin.prometheus.metrics

import com.reposilite.plugin.prometheus.specification.PrometheusPluginSpecification
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class QueuedThreadPoolMetricsTest : PrometheusPluginSpecification(){
    private val threadPool = FakeQueuedThreadPool()
    private val registry = PrometheusRegistry()

    @Test
    fun `should initialize without failing`() {
        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
        }
    }

    @Test
    fun `all metrics should be initialized`() {
        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
        }

        val snapshots = registry.scrape()

        assertThat(snapshots.map { it.metadata.prometheusName }).contains(
            "jetty_queued_thread_pool_jobs",
            "jetty_queued_thread_pool_low_on_threads",
            "jetty_queued_thread_pool_threads_state",
            "jetty_queued_thread_pool_utilization_ratio",
        )
    }

    @Test
    fun `should not fail with NaN statistics`() {
        threadPool._utilizationRate = Double.NaN

        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with MAX_VALUE statistics`() {
        threadPool._threads = Int.MAX_VALUE
        threadPool._idleThreads = Int.MAX_VALUE
        threadPool._minThreads = Int.MAX_VALUE
        threadPool._maxThreads = Int.MAX_VALUE
        threadPool._readyThreads = Int.MAX_VALUE
        threadPool._busyThreads = Int.MAX_VALUE
        threadPool._utilizedThreads = Int.MAX_VALUE
        threadPool._utilizationRate = Double.MAX_VALUE
        threadPool._queueSize = Int.MAX_VALUE

        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with MIN_VALUE statistics`() {
        threadPool._threads = Int.MIN_VALUE
        threadPool._idleThreads = Int.MIN_VALUE
        threadPool._minThreads = Int.MIN_VALUE
        threadPool._maxThreads = Int.MIN_VALUE
        threadPool._readyThreads = Int.MIN_VALUE
        threadPool._busyThreads = Int.MIN_VALUE
        threadPool._utilizedThreads = Int.MIN_VALUE
        threadPool._utilizationRate = Double.MIN_VALUE
        threadPool._queueSize = Int.MIN_VALUE

        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with 0 statistics`() {
        threadPool._threads = 0
        threadPool._idleThreads = 0
        threadPool._minThreads = 0
        threadPool._maxThreads = 0
        threadPool._readyThreads = 0
        threadPool._busyThreads = 0
        threadPool._utilizedThreads = 0
        threadPool._utilizationRate = 0.0
        threadPool._queueSize = 0

        assertDoesNotThrow {
            QueuedThreadPoolMetrics.register(threadPool, registry)
            registry.scrape()
        }
    }

    @Suppress("PropertyName")
    class FakeQueuedThreadPool : QueuedThreadPool() {
        var _threads: Int = 1
        var _idleThreads: Int = 1
        var _minThreads: Int = 1
        var _maxThreads: Int = 1
        var _readyThreads: Int = 1
        var _busyThreads: Int = 1
        var _utilizedThreads: Int = 1
        var _utilizationRate: Double = 1.0
        var _queueSize: Int = 1

        override fun getThreads(): Int = _threads
        override fun getIdleThreads(): Int = _idleThreads
        override fun getMinThreads(): Int = _minThreads
        override fun getMaxThreads(): Int = _maxThreads
        override fun getReadyThreads(): Int = _readyThreads
        override fun getBusyThreads(): Int = _busyThreads
        override fun getUtilizedThreads(): Int = _utilizedThreads
        override fun getUtilizationRate(): Double = _utilizationRate
        override fun getQueueSize(): Int = _queueSize
    }
}