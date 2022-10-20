package com.reposilite.plugin.checksum.specification

import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.checksum.ChecksumPlugin
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal open class ChecksumPluginSpecification {

    @TempDir
    lateinit var workingDirectory: File

    private val logger = InMemoryLogger()
    private val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)))
    protected val migrationPlugin = ChecksumPlugin()

    init {
        ReposilitePluginAccessor.injectExtension(migrationPlugin, extensions)
    }

    fun workingDirectory(): Path =
        workingDirectory.toPath()

}