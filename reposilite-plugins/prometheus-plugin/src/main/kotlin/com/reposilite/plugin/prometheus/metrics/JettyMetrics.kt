package com.reposilite.plugin.prometheus.metrics

import io.prometheus.metrics.core.metrics.CounterWithCallback
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.core.metrics.Summary
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.eclipse.jetty.io.Connection
import org.eclipse.jetty.server.handler.StatisticsHandler
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit


object JettyMetrics : Connection.Listener {
    lateinit var responseTimeSummary: Summary
        private set

    private lateinit var requestBytesSummary: Summary

    private lateinit var responseBytesSummary: Summary

    fun register(statisticsHandler: StatisticsHandler, registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry) {
        CounterWithCallback.builder()
            .name("jetty_requests_total")
            .help("Number of requests")
            .callback { callback -> callback.call(statisticsHandler.requests.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_requests_active")
            .help("Number of requests currently active")
            .callback { callback -> callback.call(statisticsHandler.requestsActive.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_requests_active_max")
            .help("Maximum number of requests that have been active at once")
            .callback { callback -> callback.call(statisticsHandler.requestsActiveMax.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_max")
            .help("Maximum time spent handling requests")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeMax.toMillisFinite()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_request_time_seconds_total")
            .help("Total time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeTotal.toMillisFinite()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_mean")
            .help("Mean time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeMean.toMillisFinite()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_stddev")
            .help("Standard deviation of time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeStdDev.toMillisFinite()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_dispatched_total")
            .help("Number of dispatches")
            .callback { callback -> callback.call(statisticsHandler.dispatched.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_dispatched_active")
            .help("Number of dispatches currently active")
            .callback { callback -> callback.call(statisticsHandler.dispatchedActive.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_dispatched_active_max")
            .help("Maximum number of active dispatches being handled")
            .callback { callback -> callback.call(statisticsHandler.dispatchedActiveMax.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_max")
            .help("Maximum time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeMax.toMillisFinite()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_dispatched_time_seconds_total")
            .help("Total time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeTotal.toMillisFinite()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_mean")
            .help("Mean time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeMean.toMillisFinite()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_stddev")
            .help("Standard deviation of time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeStdDev.toMillisFinite()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_async_requests_total")
            .help("Total number of async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequests.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_async_requests_waiting")
            .help("Currently waiting async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequestsWaiting.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_async_requests_waiting_max")
            .help("Maximum number of waiting async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequestsWaitingMax.toDouble()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_async_dispatches_total")
            .help("Number of requested that have been asynchronously dispatched")
            .callback { callback -> callback.call(statisticsHandler.asyncDispatches.toDouble()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_expires_total")
            .help("Number of async requests requests that have expired")
            .callback { callback -> callback.call(statisticsHandler.expires.toDouble()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_errors_total")
            .help("Number of async errors that occurred")
            .callback { callback -> callback.call(statisticsHandler.errors.toDouble()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_responses_total")
            .help("Number of requests with response status")
            .labelNames("code")
            .callback { callback ->
                callback.call(statisticsHandler.responses1xx.toDouble(), "1xx")
                callback.call(statisticsHandler.responses2xx.toDouble(), "2xx")
                callback.call(statisticsHandler.responses3xx.toDouble(), "3xx")
                callback.call(statisticsHandler.responses4xx.toDouble(), "4xx")
                callback.call(statisticsHandler.responses5xx.toDouble(), "5xx")
            }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_responses_thrown_total")
            .help("Number of requests that threw an exception")
            .callback { callback -> callback.call(statisticsHandler.responsesThrown.toDouble()) }
            .register(registry)

        GaugeWithCallback.builder()
            .name("jetty_stats_seconds")
            .help("Time in seconds stats have been collected for")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.statsOnMs.toMillisFinite()) }
            .register(registry)

        CounterWithCallback.builder()
            .name("jetty_responses_bytes_total")
            .help("Total number of bytes across all responses")
            .callback { callback -> callback.call(statisticsHandler.responsesBytesTotal.toDouble()) }
            .register(registry)

        responseTimeSummary = Summary.builder()
            .name("jetty_response_time_seconds")
            .help("Time spent for a response")
            .labelNames("code")
            .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
            .register(registry)

        requestBytesSummary = Summary.builder()
            .name("jetty_request_bytes")
            .help("Size in bytes of incoming requests")
            .unit(MetricsUnit.BYTES)
            .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
            .register(registry)

        responseBytesSummary = Summary.builder()
            .name("jetty_response_bytes")
            .help("Size in bytes of outgoing responses")
            .unit(MetricsUnit.BYTES)
            .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
            .register(registry)
    }

    override fun onOpened(connection: Connection) {}

    override fun onClosed(connection: Connection) {
        requestBytesSummary.observe(connection.bytesIn.toDouble())
        responseBytesSummary.observe(connection.bytesOut.toDouble())
    }

    private fun Double.toMillisFinite() = if (this.isFinite()) this.milliseconds.toDouble(DurationUnit.SECONDS) else this

    private fun Long.toMillisFinite() = this.milliseconds.toDouble(DurationUnit.SECONDS)
}
