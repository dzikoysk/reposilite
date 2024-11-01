/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.packages.maven.specification

import com.reposilite.auth.application.AuthenticationComponents
import com.reposilite.auth.application.AuthenticationSettings
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.packages.maven.MavenFacade
import com.reposilite.packages.maven.MavenRepository
import com.reposilite.packages.maven.api.LookupRequest
import com.reposilite.packages.maven.api.Metadata
import com.reposilite.packages.maven.api.SaveMetadataRequest
import com.reposilite.packages.maven.api.Versioning
import com.reposilite.packages.maven.application.MavenComponents
import com.reposilite.packages.maven.application.MavenSettings
import com.reposilite.packages.maven.application.MirrorCredentials
import com.reposilite.packages.maven.application.RepositorySettings
import com.reposilite.plugin.Extensions
import com.reposilite.shared.http.AuthenticationMethod.BASIC
import com.reposilite.shared.http.FakeRemoteClientProvider
import com.reposilite.shared.notFoundError
import com.reposilite.statistics.DailyDateIntervalProvider
import com.reposilite.statistics.StatisticsFacade
import com.reposilite.statistics.infrastructure.InMemoryStatisticsRepository
import com.reposilite.status.application.FailureComponents
import com.reposilite.storage.StorageFacade
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.token.AccessTokenIdentifier
import com.reposilite.token.AccessTokenType.TEMPORARY
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.CreateAccessTokenRequest
import com.reposilite.token.application.AccessTokenComponents
import io.javalin.http.ContentType.TEXT_XML
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import panda.std.Quad
import panda.std.asSuccess
import panda.std.reactive.Reference
import panda.std.reactive.reference
import panda.std.reactive.toReference
import java.io.File
import java.nio.file.Files
import java.time.Clock

internal abstract class MavenSpecification {

    protected companion object {
        val UNAUTHORIZED: AccessTokenIdentifier? = null
        const val REMOTE_REPOSITORY = "https://domain.com/releases"
        const val REMOTE_REPOSITORY_WITH_WHITELIST = "https://example.com/whitelist"
        val REMOTE_AUTH = MirrorCredentials(BASIC, "panda", "secret")
        const val REMOTE_CONTENT = "content"
    }

    @TempDir
    lateinit var workingDirectory: File
    protected lateinit var mavenFacade: MavenFacade

    private val clock = Clock.systemDefaultZone()
    private val logger = InMemoryLogger()
    protected val extensions = Extensions(logger)

    private val failureFacade = FailureComponents(
        journalist = logger
    ).failureFacade()

    private val accessTokenFacade = AccessTokenComponents(
        journalist = logger,
        database = null
    ).accessTokenFacade()

    private val authenticationFacade = AuthenticationComponents(
        journalist = logger,
        accessTokenFacade = accessTokenFacade,
        failureFacade = failureFacade,
        authenticationSettings = AuthenticationSettings().toReference(),
    ).authenticationFacade()

    abstract fun repositories(): List<RepositorySettings>

    @BeforeEach
    fun initializeFacade() {
        val remoteClientProvider = FakeRemoteClientProvider(
            headHandler = { uri, credentials, _, _ ->
                if (uri.startsWith(REMOTE_REPOSITORY) && REMOTE_AUTH == credentials && !uri.isAllowed())
                    DocumentInfo(
                        name = uri.toLocation().getSimpleName(),
                        contentType = TEXT_XML,
                    ).asSuccess()
                else if (uri.startsWith(REMOTE_REPOSITORY_WITH_WHITELIST) && uri.isAllowed())
                    DocumentInfo(
                        name = uri.toLocation().getSimpleName(),
                        contentType = TEXT_XML,
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

        this.mavenFacade = MavenComponents(
            clock = clock,
            workingDirectory = workingDirectory.toPath(),
            journalist = logger,
            extensions = extensions,
            remoteClientProvider = remoteClientProvider,
            failureFacade = failureFacade,
            storageFacade = StorageFacade(),
            authenticationFacade = authenticationFacade,
            accessTokenFacade = accessTokenFacade,
            statisticsFacade = StatisticsFacade(logger, Reference.reference(false), DailyDateIntervalProvider.toReference(), InMemoryStatisticsRepository()),
            mavenSettings = reference(MavenSettings(
                repositories = repositories()
            )),
            id = reference("test-repository-id")
        ).mavenFacade()
    }

    protected inner class FileSpec(
        val repository: String,
        val gav: String,
        val content: String
    ) {

        fun repository(): MavenRepository =
            mavenFacade.getRepository(repository)!!

        fun toLookupRequest(authentication: AccessTokenIdentifier?): LookupRequest =
            LookupRequest(authentication, repository, gav.toLocation())

        fun gav(): Location =
            gav.toLocation()

    }

    protected fun findRepositories(accessToken: AccessTokenIdentifier?): Collection<String> =
        mavenFacade.findRepositories(accessToken).files.map { it.name }

    protected fun addFileToRepository(fileSpec: FileSpec): FileSpec {
        workingDirectory.toPath()
            .resolve("repositories")
            .resolve(fileSpec.repository)
            .resolve(fileSpec.gav.toLocation().toPath().get())
            .also {
                Files.createDirectories(it.parent)
                Files.createFile(it)
                Files.write(it, fileSpec.content.toByteArray())
            }

        return fileSpec
    }

    protected fun createAccessToken(name: String, secret: String, repository: String, gav: String, permission: RoutePermission): AccessTokenIdentifier =
        accessTokenFacade.createAccessToken(CreateAccessTokenRequest(TEMPORARY, name, secret = secret))
            .accessToken
            .also { accessTokenFacade.addRoute(it.identifier, Route("/$repository/${gav.toLocation()}", permission)) }
            .identifier

    private fun String.isAllowed(): Boolean =
        this.endsWith("/allow")

    protected fun useMetadata(repository: String, gav: String, versioning: List<String>, filter: String? = null): Quad<MavenRepository, Location, Metadata, String> =
        Quad(
            mavenFacade.getRepository(repository),
            gav.toLocation(),
            mavenFacade.saveMetadata(
                SaveMetadataRequest(
                    mavenRepository = mavenFacade.getRepository(repository)!!,
                    gav = gav.toLocation(),
                    metadata = Metadata(versioning = Versioning(_versions = versioning))
                )
            ).get(),
            filter
        )

}
