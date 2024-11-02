package com.reposilite.plugin.prometheus.metrics

import com.reposilite.status.FailureFacade
import com.reposilite.status.StatusFacade
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.CounterWithCallback
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.core.metrics.Summary
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import io.prometheus.metrics.model.snapshots.Unit as MetricsUnit

object ReposiliteMetrics {
    // TODO: Remove this? See #2251
    val responseFileSizeSummary: Summary = Summary.builder()
        .name("reposilite_response_file_size_bytes")
        .help("Size in bytes of response files")
        .unit(MetricsUnit.BYTES)
        .quantile(0.01, 0.05, 0.1, 0.5, 0.9, 0.95, 0.99)
        .register()

    // TODO: Remove this? See #2251
    val resolvedFileCounter: Counter = Counter.builder()
        .name("reposilite_resolved_total")
        .help("Total resolved files count")
        .register()

    val mavenDeployCounter: Counter = Counter.builder()
        .name("reposilite_deploy_total")
        .help("Total successful deployments count")
        .register()

    val responseCounter: Counter = Counter.builder()
        .name("reposilite_responses_total")
        .help("Total response count, filtered to exclude /metrics")
        .labelNames("code")
        .register()

    fun register(statusFacade: StatusFacade, failureFacade: FailureFacade) {
        GaugeWithCallback.builder()
            .name("reposilite_uptime_seconds")
            .help("Uptime of reposilite")
            .unit(MetricsUnit.SECONDS)
            .callback { callback -> callback.call(statusFacade.fetchInstanceStatus().uptime.milliseconds.toDouble(DurationUnit.SECONDS)) }
            .register()

        CounterWithCallback.builder()
            .name("reposilite_failures_total")
            .help("Number of failures reposilite has encountered")
            .callback { callback -> callback.call(failureFacade.getFailures().size.toDouble()) }
            .register()
    }
}
