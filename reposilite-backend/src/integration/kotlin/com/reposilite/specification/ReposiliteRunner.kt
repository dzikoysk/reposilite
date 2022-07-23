/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.specification

import com.reposilite.Reposilite
import com.reposilite.ReposiliteFactory
import com.reposilite.ReposiliteParameters
import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.configuration.local.infrastructure.LOCAL_CONFIGURATION_FILE
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.journalist.Channel
import com.reposilite.journalist.Logger
import com.reposilite.journalist.backend.PrintStreamLogger
import com.reposilite.maven.application.MavenSettings
import com.reposilite.maven.application.ProxiedRepository
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.storage.StorageProviderSettings
import io.javalin.util.JavalinBindException
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
 * - [LocalSpecificationJunitExtension]
 * - [RemoteSpecificationJunitExtension]
 */
@Suppress("PropertyName")
internal abstract class ReposiliteRunner {

    companion object {
        val DEFAULT_TOKEN = "manager" to "manager-secret"
    }

    @TempDir
    lateinit var reposiliteWorkingDirectory: File
    @JvmField
    var _extensionInitialized = false
    @JvmField
    var _database: String = ""
    @JvmField
    var _storageProvider: StorageProviderSettings? = null

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
            launchResult = prepareInstance(logger)
        } while (launchResult.errorToOption().`is`(JavalinBindException::class.java).isPresent)
    }

    private fun prepareInstance(logger: Logger): Result<Reposilite, Exception> {
        val parameters = ReposiliteParameters()
        parameters.tokenEntries = arrayOf("${DEFAULT_TOKEN.first}:${DEFAULT_TOKEN.second}")
        parameters.workingDirectoryName = reposiliteWorkingDirectory.absolutePath
        parameters.testEnv = true
        parameters.port = 10000 + 2 * ThreadLocalRandom.current().nextInt(30_000 / 2)
        parameters.run()
        overrideParameters(parameters)

        val localConfiguration = LocalConfiguration().also {
            ReferenceUtils.setValue(it.database, _database)
            ReferenceUtils.setValue(it.webThreadPool, 5)
            ReferenceUtils.setValue(it.ioThreadPool, 2)
        }

        overrideLocalConfiguration(localConfiguration)
        KCdnFactory.createStandard().render(localConfiguration, Source.of(reposiliteWorkingDirectory.resolve(LOCAL_CONFIGURATION_FILE)))

        reposilite = ReposiliteFactory.createReposilite(parameters, logger)
        reposilite.journalist.setVisibleThreshold(Channel.WARN)

        val sharedConfigurationFacade = reposilite.extensions.facade<SharedConfigurationFacade>()

        sharedConfigurationFacade.getDomainSettings<MavenSettings>().update { old ->
            val proxiedConfiguration = RepositorySettings(
                id = "proxied",
                proxied = mutableListOf(ProxiedRepository("http://localhost:${parameters.port + 1}/releases"))
            )

            return@update old.copy(
                repositories = old.repositories.associateBy { it.id }.toMutableMap()
                    .also { repositories -> repositories["proxied"] = proxiedConfiguration }
                    .mapValues { (_, repositoryConfiguration) ->
                        repositoryConfiguration.copy(
                            redeployment = true,
                            storageProvider = _storageProvider!!,
                        )
                    }.values.toList()
            )
        }

        overrideSharedConfiguration(sharedConfigurationFacade)
        return reposilite.launch()
    }

    protected open fun overrideParameters(parameters: ReposiliteParameters) { }

    protected open fun overrideLocalConfiguration(localConfiguration: LocalConfiguration) { }

    protected open fun overrideSharedConfiguration(sharedConfigurationFacade: SharedConfigurationFacade) { }

    @AfterEach
    fun shutdownApplication() {
        reposilite.shutdown()
    }

}
