/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite

import com.reposilite.config.Configuration
import com.reposilite.config.Configuration.RepositoryConfiguration
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
    protected var proxiedPort: Int = PORT_ASSIGNER.incrementAndGet()
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

        val proxiedConfiguration = RepositoryConfiguration()
        proxiedConfiguration.proxied = mutableListOf("http://localhost:$proxiedPort/releases")
        configuration.repositories["proxied"] = proxiedConfiguration

        configuration.repositories.forEach { (repositoryName, repositoryConfiguration) ->
            repositoryConfiguration.redeployment = true
            repositoryConfiguration.storageProvider = _storageProvider.replace("{repository}", repositoryName)
        }

        configuration.webThreadPool = 4
        configuration.ioThreadPool = 2

        reposilite = ReposiliteFactory.createReposilite(parameters, logger, configuration)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)
        reposilite.launch()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

}