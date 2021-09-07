package com.reposilite

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.BindException
import java.util.concurrent.ThreadLocalRandom
import kotlin.Short.Companion.MAX_VALUE

internal abstract class ReposiliteSpec {

    @TempDir
    lateinit var reposiliteWorkingDirectory: File

    private var port: Int = -1

    protected val base: String
        get() = "http://localhost:$port"

    private lateinit var reposilite: Reposilite

    @BeforeEach
    protected fun bootApplication() {
        tryCreateReposilite()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

    private fun tryCreateReposilite() {
        try {
            createReposilite()
        } catch (portUnavailable : BindException) {
            tryCreateReposilite()
        }
    }

    private fun createReposilite() {
        this.port = ThreadLocalRandom.current().nextInt(1024, MAX_VALUE - 1025)

        val parameters = ReposiliteParameters()
        parameters.port = port
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.tokenEntries = arrayOf("name:secret")
        parameters.run()

        this.reposilite = ReposiliteWebConfiguration.createReposilite(parameters)
        reposilite.launch()
    }

}