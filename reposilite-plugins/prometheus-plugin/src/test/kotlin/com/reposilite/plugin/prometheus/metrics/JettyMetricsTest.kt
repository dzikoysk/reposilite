package com.reposilite.plugin.prometheus.metrics

import com.reposilite.plugin.prometheus.specification.PrometheusPluginSpecification
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class JettyMetricsTest : PrometheusPluginSpecification() {
    private val statistics = FakeStatisticsHandler()
    private val registry = PrometheusRegistry()

    @Test
    fun `should initialize without failing`() {
        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
        }
    }

    @Test
    fun `all metrics should be initialized`() {
        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
        }

        val snapshots = registry.scrape()

        assertThat(snapshots.map { it.metadata.prometheusName }).contains(
            "jetty_requests",
            "jetty_requests_active",
            "jetty_requests_active_max",
            "jetty_request_time_seconds",
            "jetty_request_time_seconds_max_seconds",
            "jetty_request_time_seconds_mean_seconds",
            "jetty_request_time_seconds_stddev_seconds",
            "jetty_dispatched",
            "jetty_dispatched_active",
            "jetty_dispatched_active_max",
            "jetty_dispatched_time_seconds",
            "jetty_dispatched_time_seconds_max_seconds",
            "jetty_dispatched_time_seconds_mean_seconds",
            "jetty_dispatched_time_seconds_stddev_seconds",
            "jetty_async_requests",
            "jetty_async_requests_waiting",
            "jetty_async_requests_waiting_max",
            "jetty_async_dispatches",
            "jetty_expires",
            "jetty_errors",
            "jetty_responses",
            "jetty_responses_thrown",
            "jetty_stats_seconds",
            "jetty_responses_bytes",
            "jetty_response_time_seconds",
        )
    }

    @Test
    fun `should not fail with NaN statistics`() {
        statistics._requestTimeMean = Double.NaN
        statistics._requestTimeStdDev = Double.NaN
        statistics._dispatchedTimeMean = Double.NaN
        statistics._dispatchedTimeStdDev = Double.NaN

        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with MAX_VALUE statistics`() {
        statistics._requests = Int.MAX_VALUE
        statistics._requestsActive = Int.MAX_VALUE
        statistics._requestsActiveMax = Int.MAX_VALUE
        statistics._requestTimeMax = Long.MAX_VALUE
        statistics._requestTimeTotal = Long.MAX_VALUE
        statistics._requestTimeMean = Double.MAX_VALUE
        statistics._requestTimeStdDev = Double.MAX_VALUE
        statistics._dispatched = Int.MAX_VALUE
        statistics._dispatchedActive = Int.MAX_VALUE
        statistics._dispatchedActiveMax = Int.MAX_VALUE
        statistics._dispatchedTimeMax = Long.MAX_VALUE
        statistics._dispatchedTimeTotal = Long.MAX_VALUE
        statistics._dispatchedTimeMean = Double.MAX_VALUE
        statistics._dispatchedTimeStdDev = Double.MAX_VALUE
        statistics._asyncRequests = Int.MAX_VALUE
        statistics._asyncRequestsWaiting = Int.MAX_VALUE
        statistics._asyncRequestsWaitingMax = Int.MAX_VALUE
        statistics._asyncDispatches = Int.MAX_VALUE
        statistics._expires = Int.MAX_VALUE
        statistics._errors = Int.MAX_VALUE
        statistics._responses1xx = Int.MAX_VALUE
        statistics._responses2xx = Int.MAX_VALUE
        statistics._responses3xx = Int.MAX_VALUE
        statistics._responses4xx = Int.MAX_VALUE
        statistics._responses5xx = Int.MAX_VALUE
        statistics._responsesThrown = Int.MAX_VALUE
        statistics._statsOnMs = Long.MAX_VALUE
        statistics._responsesBytesTotal = Long.MAX_VALUE

        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with MIN_VALUE statistics`() {
        // Missing statistics: counters must be >= 0
        statistics._requestsActive = Int.MIN_VALUE
        statistics._requestsActiveMax = Int.MIN_VALUE
        statistics._requestTimeMax = Long.MIN_VALUE
        statistics._requestTimeMean = Double.MIN_VALUE
        statistics._requestTimeStdDev = Double.MIN_VALUE
        statistics._dispatchedActive = Int.MIN_VALUE
        statistics._dispatchedActiveMax = Int.MIN_VALUE
        statistics._dispatchedTimeMax = Long.MIN_VALUE
        statistics._dispatchedTimeMean = Double.MIN_VALUE
        statistics._dispatchedTimeStdDev = Double.MIN_VALUE
        statistics._asyncRequestsWaiting = Int.MIN_VALUE
        statistics._asyncRequestsWaitingMax = Int.MIN_VALUE
        statistics._statsOnMs = Long.MIN_VALUE

        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
            registry.scrape()
        }
    }

    @Test
    fun `should not fail with 0 statistics`() {
        statistics._requests = 0
        statistics._requestsActive = 0
        statistics._requestsActiveMax = 0
        statistics._requestTimeMax = 0
        statistics._requestTimeTotal = 0
        statistics._requestTimeMean = 0.0
        statistics._requestTimeStdDev = 0.0
        statistics._dispatched = 0
        statistics._dispatchedActive = 0
        statistics._dispatchedActiveMax = 0
        statistics._dispatchedTimeMax = 0
        statistics._dispatchedTimeTotal = 0
        statistics._dispatchedTimeMean = 0.0
        statistics._dispatchedTimeStdDev = 0.0
        statistics._asyncRequests = 0
        statistics._asyncRequestsWaiting = 0
        statistics._asyncRequestsWaitingMax = 0
        statistics._asyncDispatches = 0
        statistics._expires = 0
        statistics._errors = 0
        statistics._responses1xx = 0
        statistics._responses2xx = 0
        statistics._responses3xx = 0
        statistics._responses4xx = 0
        statistics._responses5xx = 0
        statistics._responsesThrown = 0
        statistics._statsOnMs = 0
        statistics._responsesBytesTotal = 0

        assertDoesNotThrow {
            JettyMetrics.register(statistics, registry)
            registry.scrape()
        }
    }

    @Suppress("PropertyName")
    class FakeStatisticsHandler : StatisticsHandler() {
        var _requests: Int = 1
        var _requestsActive: Int = 1
        var _requestsActiveMax: Int = 1
        var _requestTimeMax: Long = 1
        var _requestTimeTotal: Long = 1
        var _requestTimeMean: Double = 1.0
        var _requestTimeStdDev: Double = 1.0
        var _dispatched: Int = 1
        var _dispatchedActive: Int = 1
        var _dispatchedActiveMax: Int = 1
        var _dispatchedTimeMax: Long = 1
        var _dispatchedTimeTotal: Long = 1
        var _dispatchedTimeMean: Double = 1.0
        var _dispatchedTimeStdDev: Double = 1.0
        var _asyncRequests: Int = 1
        var _asyncRequestsWaiting: Int = 1
        var _asyncRequestsWaitingMax: Int = 1
        var _asyncDispatches: Int = 1
        var _expires: Int = 1
        var _errors: Int = 1
        var _responses1xx: Int = 1
        var _responses2xx: Int = 1
        var _responses3xx: Int = 1
        var _responses4xx: Int = 1
        var _responses5xx: Int = 1
        var _responsesThrown: Int = 1
        var _statsOnMs: Long = 1
        var _responsesBytesTotal: Long = 1

        override fun getRequests(): Int = _requests
        override fun getRequestsActive(): Int = _requestsActive
        override fun getRequestsActiveMax(): Int = _requestsActiveMax
        override fun getRequestTimeMax(): Long = _requestTimeMax
        override fun getRequestTimeTotal(): Long = _requestTimeTotal
        override fun getRequestTimeMean(): Double = _requestTimeMean
        override fun getRequestTimeStdDev(): Double = _requestTimeStdDev
        override fun getDispatched(): Int = _dispatched
        override fun getDispatchedActive(): Int = _dispatchedActive
        override fun getDispatchedActiveMax(): Int = _dispatchedActiveMax
        override fun getDispatchedTimeMax(): Long = _dispatchedTimeMax
        override fun getDispatchedTimeTotal(): Long = _dispatchedTimeTotal
        override fun getDispatchedTimeMean(): Double = _dispatchedTimeMean
        override fun getDispatchedTimeStdDev(): Double = _dispatchedTimeStdDev
        override fun getAsyncRequests(): Int = _asyncRequests
        override fun getAsyncRequestsWaiting(): Int = _asyncRequestsWaiting
        override fun getAsyncRequestsWaitingMax(): Int = _asyncRequestsWaitingMax
        override fun getAsyncDispatches(): Int = _asyncDispatches
        override fun getExpires(): Int = _expires
        override fun getErrors(): Int = _errors
        override fun getResponses1xx(): Int = _responses1xx
        override fun getResponses2xx(): Int = _responses2xx
        override fun getResponses3xx(): Int = _responses3xx
        override fun getResponses4xx(): Int = _responses4xx
        override fun getResponses5xx(): Int = _responses5xx
        override fun getResponsesThrown(): Int = _responsesThrown
        override fun getStatsOnMs(): Long = _statsOnMs
        override fun getResponsesBytesTotal(): Long = _responsesBytesTotal
    }
}