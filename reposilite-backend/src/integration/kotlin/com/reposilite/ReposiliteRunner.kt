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
import com.reposilite.journalist.Logger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration
import io.javalin.core.util.JavalinBindException
import net.dzikoysk.cdn.KCdnFactory
import net.dzikoysk.cdn.source.Source
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.Result
import panda.std.reactive.ReferenceUtils
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.ThreadLocalRandom

/**
 * This is a dirty launcher of Reposilite instance for integration tests.
 * Every integration test is launched twice, with local and remote integrations, through dedicated extensions:
 * - [ReposiliteLocalIntegrationJunitExtension]
 * - [ReposiliteRemoteIntegrationJunitExtension]
 */
@Suppress("PropertyName")
internal abstract class ReposiliteRunner {

    companion object {
        val DEFAULT_TOKEN = Pair("manager", "manager-secret")
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    @JvmField
    var _extensionInitialized = false
    @JvmField
    var _database: String = ""
    @JvmField
    var _storageProvider = ""

    lateinit var reposilite: Reposilite

    @BeforeEach
    fun bootApplication() {
        if (!_extensionInitialized) {
            throw IllegalStateException("Missing Reposilite extension on integration test")
        }

        // disable log.txt to avoid conflicts with parallel testing
        System.setProperty("tinylog.writerFile.level", "off")
        val logger = PrintStreamLogger(PrintStream(Files.createTempFile("reposilite", "test-out").toFile()), System.err, Channel.ALL, false)
        var launchResult: Result<Reposilite, Exception>

        do {
            launchResult = prepareInstance(logger).peek { reposilite = it }
        } while (launchResult.errorToOption().`is`(JavalinBindException::class.java).isPresent)
    }

    private fun prepareInstance(logger: Logger): Result<Reposilite, Exception> {
        val parameters = ReposiliteParameters()
        parameters.sharedConfigurationMode = "copy"
        parameters.tokenEntries = arrayOf("${DEFAULT_TOKEN.first}:${DEFAULT_TOKEN.second}")
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.testEnv = true
        parameters.port = 10000 + 2 * ThreadLocalRandom.current().nextInt(30_000 / 2)
        parameters.run()

        val localConfiguration = LocalConfiguration().also {
            ReferenceUtils.setValue(it.database, _database)
            ReferenceUtils.setValue(it.webThreadPool, 5)
            ReferenceUtils.setValue(it.ioThreadPool, 2)
        }

        val cdn = KCdnFactory.createStandard()
        cdn.render(localConfiguration, Source.of(reposiliteWorkingDirectory.resolve("configuration.local.cdn")))

        val sharedConfiguration = SharedConfiguration().also {
            val proxiedConfiguration = RepositoryConfiguration()
            proxiedConfiguration.proxied = mutableListOf("http://localhost:${parameters.port + 1}/releases")

            val updatedRepositories = it.repositories.get().toMutableMap()
            updatedRepositories["proxied"] = proxiedConfiguration
            it.repositories.update(updatedRepositories)

            it.repositories.get().forEach { (repositoryName, repositoryConfiguration) ->
                repositoryConfiguration.redeployment = true
                repositoryConfiguration.storageProvider = _storageProvider.replace("{repository}", repositoryName)
            }
        }

        cdn.render(sharedConfiguration, Source.of(reposiliteWorkingDirectory.resolve("configuration.shared.cdn")))

        val reposiliteInstance = ReposiliteFactory.createReposilite(parameters, logger)
        reposiliteInstance.journalist.setVisibleThreshold(Channel.WARN)

        return reposiliteInstance.launch()
    }

    @AfterEach
    fun shutdownApplication() {
        reposilite.shutdown()
    }

}