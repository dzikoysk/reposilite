package com.reposilite.plugin.prometheus.metrics

import com.reposilite.plugin.prometheus.specification.PrometheusPluginSpecification
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Duration

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
        statistics._handleTimeMean = Double.NaN
        statistics._handleTimeStdDev = Double.NaN

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
        statistics._handleTotal = Int.MAX_VALUE
        statistics._handleActive = Int.MAX_VALUE
        statistics._handleActiveMax = Int.MAX_VALUE
        statistics._handleTimeMax = Long.MAX_VALUE
        statistics._handleTimeTotal = Long.MAX_VALUE
        statistics._handleTimeMean = Double.MAX_VALUE
        statistics._handleTimeStdDev = Double.MAX_VALUE
        statistics._failures = Int.MAX_VALUE
        statistics._responses1xx = Int.MAX_VALUE
        statistics._responses2xx = Int.MAX_VALUE
        statistics._responses3xx = Int.MAX_VALUE
        statistics._responses4xx = Int.MAX_VALUE
        statistics._responses5xx = Int.MAX_VALUE
        statistics._handlingFailures = Int.MAX_VALUE
        statistics._statisticsDuration = Duration.ofMillis(Long.MAX_VALUE)
        statistics._bytesWritten = Long.MAX_VALUE

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
        statistics._handleActive = Int.MIN_VALUE
        statistics._handleActiveMax = Int.MIN_VALUE
        statistics._handleTimeMax = Long.MIN_VALUE
        statistics._handleTimeMean = Double.MIN_VALUE
        statistics._handleTimeStdDev = Double.MIN_VALUE
        statistics._statisticsDuration = Duration.ofMillis(Long.MIN_VALUE)

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
        statistics._handleTotal = 0
        statistics._handleActive = 0
        statistics._handleActiveMax = 0
        statistics._handleTimeMax = 0
        statistics._handleTimeTotal = 0
        statistics._handleTimeMean = 0.0
        statistics._handleTimeStdDev = 0.0
        statistics._failures = 0
        statistics._responses1xx = 0
        statistics._responses2xx = 0
        statistics._responses3xx = 0
        statistics._responses4xx = 0
        statistics._responses5xx = 0
        statistics._handlingFailures = 0
        statistics._statisticsDuration = Duration.ZERO
        statistics._bytesWritten = 0

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
        var _handleTotal: Int = 1
        var _handleActive: Int = 1
        var _handleActiveMax: Int = 1
        var _handleTimeMax: Long = 1
        var _handleTimeTotal: Long = 1
        var _handleTimeMean: Double = 1.0
        var _handleTimeStdDev: Double = 1.0
        var _failures: Int = 1
        var _responses1xx: Int = 1
        var _responses2xx: Int = 1
        var _responses3xx: Int = 1
        var _responses4xx: Int = 1
        var _responses5xx: Int = 1
        var _handlingFailures: Int = 1
        var _statisticsDuration: Duration = Duration.ofMillis(1)
        var _bytesWritten: Long = 1

        override fun getRequests(): Int = _requests
        override fun getRequestsActive(): Int = _requestsActive
        override fun getRequestsActiveMax(): Int = _requestsActiveMax
        override fun getRequestTimeMax(): Long = _requestTimeMax
        override fun getRequestTimeTotal(): Long = _requestTimeTotal
        override fun getRequestTimeMean(): Double = _requestTimeMean
        override fun getRequestTimeStdDev(): Double = _requestTimeStdDev
        override fun getHandleTotal(): Int = _handleTotal
        override fun getHandleActive(): Int = _handleActive
        override fun getHandleActiveMax(): Int = _handleActiveMax
        override fun getHandleTimeMax(): Long = _handleTimeMax
        override fun getHandleTimeTotal(): Long = _handleTimeTotal
        override fun getHandleTimeMean(): Double = _handleTimeMean
        override fun getHandleTimeStdDev(): Double = _handleTimeStdDev
        override fun getFailures(): Int = _failures
        override fun getResponses1xx(): Int = _responses1xx
        override fun getResponses2xx(): Int = _responses2xx
        override fun getResponses3xx(): Int = _responses3xx
        override fun getResponses4xx(): Int = _responses4xx
        override fun getResponses5xx(): Int = _responses5xx
        override fun getHandlingFailures(): Int = _handlingFailures
        override fun getStatisticsDuration(): Duration = _statisticsDuration
        override fun getBytesWritten(): Long = _bytesWritten
    }
}
