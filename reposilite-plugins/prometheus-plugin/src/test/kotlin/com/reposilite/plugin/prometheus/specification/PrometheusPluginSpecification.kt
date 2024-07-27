package com.reposilite.plugin.prometheus.specification

import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.prometheus.PrometheusPlugin
import com.reposilite.status.application.FailureComponents
import com.reposilite.status.application.StatusComponents
import panda.std.reactive.Reference

internal open class PrometheusPluginSpecification {
    private val logger = InMemoryLogger()
    private val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)))
    protected val prometheusPlugin = PrometheusPlugin()

    init {
        extensions.registerFacade(FailureComponents(logger).failureFacade())
        extensions.registerFacade(StatusComponents(true, extensions.facade(), "", { true }, Reference(1)).statusFacade())
        ReposilitePluginAccessor.injectExtension(prometheusPlugin, extensions)
    }
}
