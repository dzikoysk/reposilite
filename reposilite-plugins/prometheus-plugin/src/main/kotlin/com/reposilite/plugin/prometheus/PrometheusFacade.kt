package com.reposilite.plugin.prometheus

import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.status.FailureFacade
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import panda.std.Result
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class PrometheusFacade(
    private val failureFacade: FailureFacade,
    private val prometheusUser: String,
    private val prometheusPassword: String
) {

    fun hasAccess(username: String, password: String): Boolean =
        username == prometheusUser && password == prometheusPassword

    fun getMetrics(acceptedType: String?, names: Set<String>): Result<PrometheusMetricsResponse, ErrorResponse> =
        Result.supplyThrowing {
            val contentType = TextFormat.chooseContentType(acceptedType)
            val output = ByteArrayOutputStream()

            PrintWriter(output).use {
                TextFormat.writeFormat(
                    contentType,
                    it,
                    CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names)
                )
            }

            PrometheusMetricsResponse(
                contentType = contentType,
                content = output.toByteArray().inputStream()
            )
        }
        .onError { failureFacade.throwException("prometheus-metrics-endpoint", it) }
        .mapErr { badRequest("Cannot process this request") }

}