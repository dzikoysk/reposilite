package com.reposilite.plugin.migration.specification

import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.backend.AggregatedLogger
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.plugin.Extensions
import com.reposilite.plugin.api.ReposilitePlugin.ReposilitePluginAccessor
import com.reposilite.plugin.migration.MigrationPlugin
import com.reposilite.configuration.api.LocalConfiguration
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal open class MigrationPluginSpecification {

    @TempDir
    lateinit var workingDirectory: File

    private val logger = InMemoryLogger()
    private val extensions = Extensions(AggregatedLogger(logger, PrintStreamLogger(System.out, System.err)), ReposiliteParameters(), LocalConfiguration())
    protected val migrationPlugin = MigrationPlugin()

    init {
        ReposilitePluginAccessor.injectExtension(migrationPlugin, extensions)
    }

    fun workingDirectory(): Path =
        workingDirectory.toPath()

    fun resource(test: String, file: String): String =
        MigrationPluginSpecification::class.java.getResourceAsStream("/$test - $file")!!.readBytes().decodeToString()

}