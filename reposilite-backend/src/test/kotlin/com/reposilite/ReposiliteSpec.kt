package com.reposilite

import com.reposilite.journalist.Channel
import kotlinx.coroutines.runBlocking
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

        fun useAuth(): Pair<String, String> = Pair(NAME, SECRET)
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    protected lateinit var reposilite: Reposilite

    private var port: Int = -1
    protected val base: String
        get() = "http://localhost:$port"


    @BeforeEach
    protected fun bootApplication() = runBlocking {
        System.setProperty("tinylog.writerFile.level", "off") // disable log.txt to avoid conflicts with parallel testing
        tryCreateReposilite()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

    private suspend fun tryCreateReposilite() {
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

    private suspend fun createReposilite() {
        port = ThreadLocalRandom.current().nextInt(1025, MAX_VALUE - 1025)

        val parameters = ReposiliteParameters()
        parameters.testEnv = true
        parameters.port = port
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.tokenEntries = arrayOf("$NAME:$SECRET")
        parameters.run()

        reposilite = ReposiliteFactory.createReposilite(parameters)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)
        reposilite.launch()
    }

}