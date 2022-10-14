package com.reposilite.plugin.mavenindexer.specification

import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.mavenindexer.MavenIndexerPlugin

internal abstract class MavenIndexerPluginSpecification {

    private val logger = InMemoryLogger()
    private val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)))
    protected val indexerPlugin = MavenIndexerPlugin()

    init {
        ReposilitePluginAccessor.injectExtension(indexerPlugin, extensions)
    }


}