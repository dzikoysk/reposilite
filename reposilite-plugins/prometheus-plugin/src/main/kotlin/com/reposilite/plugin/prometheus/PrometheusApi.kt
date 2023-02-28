package com.reposilite.plugin.prometheus

import java.io.InputStream

data class PrometheusMetricsResponse(
    val contentType: String,
    val content: InputStream
)