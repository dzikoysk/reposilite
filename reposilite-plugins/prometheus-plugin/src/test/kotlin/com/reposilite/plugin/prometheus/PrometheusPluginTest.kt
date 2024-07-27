package com.reposilite.plugin.prometheus

import com.reposilite.plugin.prometheus.specification.PrometheusPluginSpecification
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class PrometheusPluginTest : PrometheusPluginSpecification() {

    @Test
    fun `should fetch metrics without failing`() {
        System.setProperty("reposilite.prometheus.user", "user")
        System.setProperty("reposilite.prometheus.password", "password")

        assertDoesNotThrow {
            val facade = prometheusPlugin.initialize()
            val result = facade.getMetrics(null, setOf())
            assert(result.isOk)
        }
    }
}
