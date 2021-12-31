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

package com.reposilite.maven.specification

import com.reposilite.ReposiliteParameters
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.MetadataService
import com.reposilite.maven.ProxyService
import com.reposilite.maven.RepositoryProvider
import com.reposilite.maven.RepositorySecurityProvider
import com.reposilite.maven.RepositoryService
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.Versioning
import com.reposilite.plugin.Extensions
import com.reposilite.settings.api.LocalConfiguration
import com.reposilite.settings.api.SharedConfiguration.RepositoryConfiguration
import com.reposilite.shared.fs.DocumentInfo
import com.reposilite.shared.fs.UNKNOWN_LENGTH
import com.reposilite.shared.fs.append
import com.reposilite.shared.fs.getSimpleNameFromUri
import com.reposilite.shared.fs.safeResolve
import com.reposilite.shared.http.FakeRemoteClientProvider
import com.reposilite.statistics.DailyDateIntervalProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.Route
import com.reposilite.token.api.RoutePermission
import com.reposilite.web.http.notFoundError
import io.javalin.http.ContentType.TEXT_XML
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.Quad
import panda.std.asSuccess
import panda.std.reactive.mutableReference
import panda.std.reactive.toReference
import java.io.File
import java.nio.file.Files

@Suppress("LeakingThis")
internal abstract class MavenSpecification {

    protected companion object {
        val UNAUTHORIZED: AccessToken? = null
        const val REMOTE_REPOSITORY = "https://domain.com/releases"
        const val REMOTE_REPOSITORY_WITH_WHITELIST = "https://example.com/whitelist"
        const val REMOTE_AUTH = "panda@secret"
        const val REMOTE_CONTENT = "content"
    }

    @TempDir
    @JvmField
    protected var workingDirectory: File? = null
    protected lateinit var mavenFacade: MavenFacade

    protected abstract fun repositories(): Map<String, RepositoryConfiguration>

    @BeforeEach
    private fun initializeFacade() {
        val logger = InMemoryLogger()
        val remoteClientProvider = FakeRemoteClientProvider(
            headHandler = { uri, credentials, _, _ ->
                if (uri.startsWith(REMOTE_REPOSITORY) && REMOTE_AUTH == credentials && !uri.isAllowed())
                    DocumentInfo(
                        uri.getSimpleNameFromUri(),
                        TEXT_XML,
                        UNKNOWN_LENGTH,
                    ).asSuccess()
                else if (uri.startsWith(REMOTE_REPOSITORY_WITH_WHITELIST) && uri.isAllowed())
                    DocumentInfo(
                        uri.getSimpleNameFromUri(),
                        TEXT_XML,
                        UNKNOWN_LENGTH,
                    ).asSuccess()
                else
                    notFoundError("Not found")
            },
            getHandler = { uri, credentials, _, _ ->
                if (uri.startsWith(REMOTE_REPOSITORY) && REMOTE_AUTH == credentials && !uri.isAllowed())
                    REMOTE_CONTENT.byteInputStream().asSuccess()
                else if (uri.startsWith(REMOTE_REPOSITORY_WITH_WHITELIST) && uri.isAllowed())
                    REMOTE_CONTENT.byteInputStream().asSuccess()
                else
                    notFoundError("Not found")
            }
        )

        val workingDirectoryPath = workingDirectory!!.toPath()
        val parameters = ReposiliteParameters().also { it.workingDirectory = workingDirectoryPath }
        val repositories = mutableReference(repositories())

        val securityProvider = RepositorySecurityProvider()
        val repositoryProvider = RepositoryProvider(logger, workingDirectoryPath, remoteClientProvider, repositories)
        val repositoryService = RepositoryService(logger, repositoryProvider, securityProvider)

        this.mavenFacade = MavenFacade(
            logger,
            securityProvider,
            RepositoryService(logger, repositoryProvider, securityProvider),
            ProxyService(logger),
            MetadataService(repositoryService),
            Extensions(logger, parameters, LocalConfiguration()),
            StatisticsFacade(logger, DailyDateIntervalProvider.toReference(), InMemoryStatisticsRepository())
        )
    }

    protected data class FileSpec(
        val repository: String,
        val gav: String,
        val content: String
    ) {

        fun toLookupRequest(authentication: AccessToken?): LookupRequest =
            LookupRequest(authentication, repository, gav)

    }

    protected fun createRepository(name: String, initializer: RepositoryConfiguration.() -> Unit): Pair<String, RepositoryConfiguration> =
        Pair(name, RepositoryConfiguration().also { initializer(it) })

    protected fun findRepositories(accessToken: AccessToken?): Collection<String> =
        mavenFacade.findRepositories(accessToken).files.map { it.name }

    protected fun addFileToRepository(fileSpec: FileSpec): FileSpec {
        workingDirectory!!.toPath()
            .safeResolve("repositories")
            .safeResolve(fileSpec.repository)
            .append(fileSpec.gav)
            .peek {
                Files.createDirectories(it.parent)
                Files.createFile(it)
                Files.write(it, fileSpec.content.toByteArray())
            }
            .get()

        return fileSpec
    }

    protected fun createAccessToken(name: String, secret: String, repository: String, gav: String, permission: RoutePermission): AccessToken {
        val routes = setOf(Route("/$repository/$gav", setOf(permission)))
        return AccessToken(name = name, encryptedSecret = secret, routes = routes)
    }

    private fun String.isAllowed(): Boolean =
        this.endsWith("/allow")

    protected fun useMetadata(repository: String, gav: String, versioning: List<String>, filter: String? = null): Quad<String, String, Metadata, String> =
        Quad(
            repository,
            gav,
            mavenFacade.saveMetadata(repository, gav, Metadata(versioning = Versioning(_versions = versioning))).get(),
            filter
        )

}