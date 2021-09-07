package com.reposilite

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import kotlin.Short.Companion.MAX_VALUE

internal abstract class ReposiliteSpec {

    companion object {
        const val NAME = "name"
        const val SECRET = "secret"
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    private lateinit var reposilite: Reposilite

    private var port: Int = -1
    protected val base: String
        get() = "http://localhost:$port"


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
        } catch (portUnavailable : RuntimeException) {
            if (portUnavailable.message?.contains("port") == true) {
                tryCreateReposilite()
            }
            else {
                portUnavailable.printStackTrace()
            }
        }
    }

    private fun createReposilite() {
        this.port = ThreadLocalRandom.current().nextInt(1025, MAX_VALUE - 1025)
        println(port)
        val parameters = ReposiliteParameters()
        parameters.testEnv = true
        parameters.port = port
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.tokenEntries = arrayOf("$NAME:$SECRET")
        parameters.run()

        this.reposilite = ReposiliteFactory.createReposilite(parameters)
        reposilite.launch()
    }

}