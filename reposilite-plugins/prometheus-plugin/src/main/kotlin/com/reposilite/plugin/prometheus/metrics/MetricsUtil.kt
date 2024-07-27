package com.reposilite.plugin.prometheus.metrics

import io.prometheus.metrics.core.metrics.Summary

fun Summary.Builder.quantile(vararg quantiles: Double): Summary.Builder {
    for (quantile in quantiles) {
        this.quantile(quantile)
    }
    return this
}
