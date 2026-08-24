package com.reposilite.plugin.prometheus.specification

import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.prometheus.PrometheusPlugin
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.status.application.FailureComponents
import com.reposilite.status.application.StatusComponents
import panda.std.reactive.Reference

internal open class PrometheusPluginSpecification {
    private val logger = InMemoryLogger()
    protected val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)))
    protected val prometheusPlugin = PrometheusPlugin()

    init {
        extensions.registerFacade(FailureComponents(logger).failureFacade())
        extensions.registerFacade(
            StatusComponents(
                testEnv = true,
                failureFacade = extensions.facade(),
                remoteClientProvider = HttpRemoteClientProvider(extensions),
                remoteVersionEndpoint = "",
                statusSupplier = { true },
                maxThreads = Reference(1)
            ).statusFacade()
        )
        ReposilitePluginAccessor.injectExtension(prometheusPlugin, extensions)
    }
}
