package com.reposilite

import com.reposilite.config.Configuration
import com.reposilite.journalist.Channel
import com.reposilite.journalist.backend.PrintStreamLogger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@Suppress("PropertyName")
internal abstract class ReposiliteRunner {

    companion object {
        private val PORT_ASSIGNER = AtomicInteger(1025)
        protected val DEFAULT_TOKEN = Pair("manager", "manager-secret")
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    @JvmField
    var _extensionInitialized = false
    @JvmField
    var _database: String = ""
    @JvmField
    var _storageProvider = ""

    protected var port: Int = PORT_ASSIGNER.incrementAndGet()
    protected lateinit var reposilite: Reposilite

    @BeforeEach
    protected fun bootApplication() = runBlocking {
        if (!_extensionInitialized) {
            throw IllegalStateException("Missing Reposilite extension on integration test")
        }

        // disable log.txt to avoid conflicts with parallel testing
        System.setProperty("tinylog.writerFile.level", "off")
        val logger = PrintStreamLogger(System.out, System.err, Channel.ALL, false)

        val parameters = ReposiliteParameters()
        parameters.tokenEntries = arrayOf("${DEFAULT_TOKEN.first}:${DEFAULT_TOKEN.second}")
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.port = port
        parameters.testEnv = true
        parameters.run()

        val configuration = Configuration()
        configuration.database = _database

        configuration.repositories.forEach { (repositoryName, repositoryConfiguration) ->
            repositoryConfiguration.storageProvider = _storageProvider.replace("{repository}", repositoryName)
        }

        reposilite = ReposiliteFactory.createReposilite(parameters, logger, configuration)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)
        reposilite.launch()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

}