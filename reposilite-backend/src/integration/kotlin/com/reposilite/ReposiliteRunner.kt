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

import com.reposilite.journalist.Channel
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.reactive.ReferenceUtils
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * This is a dirty launcher of Reposilite instance for integration tests.
 * Every integration test is launched twice, with local and remote integrations, through dedicated extensions:
 * - [ReposiliteLocalIntegrationJunitExtension]
 * - [ReposiliteRemoteIntegrationJunitExtension]
 */
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
    protected fun bootApplication() {
        if (!_extensionInitialized) {
            throw IllegalStateException("Missing Reposilite extension on integration test")
        }

        // disable log.txt to avoid conflicts with parallel testing
        System.setProperty("tinylog.writerFile.level", "off")
        val logger = PrintStreamLogger(System.out, System.err, Channel.ALL, false)

        val parameters = ReposiliteParameters()
        parameters.sharedConfigurationMode = "copy"
        parameters.tokenEntries = arrayOf("${DEFAULT_TOKEN.first}:${DEFAULT_TOKEN.second}")
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.port = port
        parameters.testEnv = true
        parameters.run()

        val cdn = KCdnFactory.createStandard()

        val localConfiguration = LocalConfiguration().also {
            ReferenceUtils.setValue(it.database, _database)
            ReferenceUtils.setValue(it.webThreadPool, 4)
            ReferenceUtils.setValue(it.ioThreadPool, 2)
        }

        cdn.render(localConfiguration, Source.of(reposiliteWorkingDirectory.resolve("configuration.local.cdn")))

        val sharedConfiguration = SharedConfiguration().also {
            val proxiedConfiguration = RepositoryConfiguration()
            proxiedConfiguration.proxied = mutableListOf("http://localhost:$proxiedPort/releases")

            val updatedRepositories = it.repositories.get().toMutableMap()
            updatedRepositories["proxied"] = proxiedConfiguration
            it.repositories.update(updatedRepositories)

            it.repositories.get().forEach { (repositoryName, repositoryConfiguration) ->
                repositoryConfiguration.redeployment = true
                repositoryConfiguration.storageProvider = _storageProvider.replace("{repository}", repositoryName)
            }
        }

        cdn.render(sharedConfiguration, Source.of(reposiliteWorkingDirectory.resolve("configuration.shared.cdn")))

        reposilite = ReposiliteFactory.createReposilite(parameters, logger)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)
        reposilite.launch()
    }

    @AfterEach
    protected fun shutdownApplication() {
        reposilite.shutdown()
    }

}