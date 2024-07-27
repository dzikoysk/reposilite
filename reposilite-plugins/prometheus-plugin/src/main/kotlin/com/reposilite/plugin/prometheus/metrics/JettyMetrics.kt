package com.reposilite.plugin.prometheus.metrics

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.CounterWithCallback
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.core.metrics.Summary
import org.eclipse.jetty.server.handler.StatisticsHandler
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit


object JettyMetrics {
    val responseTimeSummary: Summary = Summary.builder()
        .name("jetty_response_time_seconds")
        .help("Time spent for a response")
        .labelNames("code")
        .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
        .register()

    val responseSizeSummary: Summary = Summary.builder()
        .name("jetty_response_bytes")
        .help("Size in bytes of responses")
        .unit(MetricsUnit.BYTES)
        .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
        .register()

    val responseCounter: Counter = Counter.builder()
        .name("reposilite_responses_total")
        .help("Total response count")
        .labelNames("code")
        .register()

    fun register(statisticsHandler: StatisticsHandler) {
        CounterWithCallback.builder()
            .name("jetty_requests_total")
            .help("Number of requests")
            .callback { callback -> callback.call(statisticsHandler.requests.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_requests_active")
            .help("Number of requests currently active")
            .callback { callback -> callback.call(statisticsHandler.requestsActive.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_requests_active_max")
            .help("Maximum number of requests that have been active at once")
            .callback { callback -> callback.call(statisticsHandler.requestsActiveMax.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_max")
            .help("Maximum time spent handling requests")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeMax.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_request_time_seconds_total")
            .help("Total time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeTotal.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_mean")
            .help("Mean time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeMean.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_request_time_seconds_stddev")
            .help("Standard deviation of time spent in all request handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.requestTimeStdDev.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_dispatched_total")
            .help("Number of dispatches")
            .callback { callback -> callback.call(statisticsHandler.dispatched.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_dispatched_active")
            .help("Number of dispatches currently active")
            .callback { callback -> callback.call(statisticsHandler.dispatchedActive.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_dispatched_active_max")
            .help("Maximum number of active dispatches being handled")
            .callback { callback -> callback.call(statisticsHandler.dispatchedActiveMax.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_max")
            .help("Maximum time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeMax.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_dispatched_time_seconds_total")
            .help("Total time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeTotal.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_mean")
            .help("Mean time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeMean.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_dispatched_time_seconds_stddev")
            .help("Standard deviation of time spent in dispatch handling")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.dispatchedTimeStdDev.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_async_requests_total")
            .help("Total number of async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequests.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_async_requests_waiting")
            .help("Currently waiting async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequestsWaiting.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_async_requests_waiting_max")
            .help("Maximum number of waiting async requests")
            .callback { callback -> callback.call(statisticsHandler.asyncRequestsWaitingMax.toDouble()) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_async_dispatches_total")
            .help("Number of requested that have been asynchronously dispatched")
            .callback { callback -> callback.call(statisticsHandler.asyncDispatches.toDouble()) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_expires_total")
            .help("Number of async requests requests that have expired")
            .callback { callback -> callback.call(statisticsHandler.expires.toDouble()) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_errors_total")
            .help("Number of async errors that occurred")
            .callback { callback -> callback.call(statisticsHandler.errors.toDouble()) }
            .register()

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
            .register()

        CounterWithCallback.builder()
            .name("jetty_responses_thrown_total")
            .help("Number of requests that threw an exception")
            .callback { callback -> callback.call(statisticsHandler.responsesThrown.toDouble()) }
            .register()

        GaugeWithCallback.builder()
            .name("jetty_stats_seconds")
            .help("Time in seconds stats have been collected for")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statisticsHandler.statsOnMs.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("jetty_responses_bytes_total")
            .help("Total number of bytes across all responses")
            .callback { callback -> callback.call(statisticsHandler.responsesBytesTotal.toDouble()) }
            .register()
    }
}
