/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.plugin.prometheus

import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.status.FailureFacade
import io.prometheus.metrics.config.PrometheusProperties
import io.prometheus.metrics.expositionformats.ExpositionFormatWriter
import io.prometheus.metrics.expositionformats.ExpositionFormats
import io.prometheus.metrics.model.registry.MetricNameFilter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import panda.std.Result
import java.io.ByteArrayOutputStream

class PrometheusFacade(
    private val failureFacade: FailureFacade,
    private val prometheusUser: String,
    private val prometheusPassword: String
) : Facade {
    fun hasAccess(username: String, password: String): Boolean =
        username == prometheusUser && password == prometheusPassword

    fun getMetrics(acceptedType: String?, names: Set<String>): Result<PrometheusMetricsResponse, ErrorResponse> = Result.supplyThrowing {
        val exportFormat = exportFormatForAcceptHeader(acceptedType)
        val filter = MetricNameFilter.builder().nameMustBeEqualTo(names).build()
        val snapshots = PrometheusRegistry.defaultRegistry.scrape(filter)
        val output = ByteArrayOutputStream()

        output.use {
            exportFormat.write(it, snapshots)
        }

        PrometheusMetricsResponse(
            contentType = exportFormat.contentType,
            content = output.toByteArray().inputStream()
        )
    }
        .onError { failureFacade.throwException("prometheus-metrics-endpoint", it) }
        .mapErr { badRequest("Cannot process this request") }

    private fun exportFormatForAcceptHeader(acceptedType: String?): ExpositionFormatWriter {
        return ExpositionFormats.init(PrometheusProperties.get().exporterProperties).findWriter(acceptedType)
    }
}
