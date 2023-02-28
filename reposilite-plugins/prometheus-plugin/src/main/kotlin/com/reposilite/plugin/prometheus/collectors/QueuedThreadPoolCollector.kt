package com.reposilite.plugin.prometheus.collectors

import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import io.prometheus.client.Collector.Type.GAUGE
import org.eclipse.jetty.util.thread.QueuedThreadPool

class QueuedThreadPoolCollector private constructor(private val queuedThreadPool: QueuedThreadPool) : Collector() {

    override fun collect(): List<MetricFamilySamples> {
        return listOf(
            buildGauge("jetty_queued_thread_pool_threads", "Number of total threads", queuedThreadPool.threads.toDouble()),
            buildGauge(
                "jetty_queued_thread_pool_utilization",
                "Percentage of threads in use",
                queuedThreadPool.threads.toDouble() / queuedThreadPool.maxThreads
            ),
            buildGauge("jetty_queued_thread_pool_threads_idle", "Number of idle threads", queuedThreadPool.idleThreads.toDouble()),
            buildGauge("jetty_queued_thread_pool_jobs", "Number of total jobs", queuedThreadPool.queueSize.toDouble())
        )
    }

    companion object {
        private val EMPTY_LIST: List<String> = ArrayList()

        fun initialize(queuedThreadPool: QueuedThreadPool) {
            QueuedThreadPoolCollector(queuedThreadPool).register<Collector>()
        }

        private fun buildGauge(name: String, help: String, value: Double): MetricFamilySamples {
            return MetricFamilySamples(
                name,
                GAUGE,
                help, listOf(Sample(name, EMPTY_LIST, EMPTY_LIST, value))
            )
        }
    }

}
