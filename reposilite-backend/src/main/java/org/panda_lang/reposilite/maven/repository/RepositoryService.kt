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
package org.panda_lang.reposilite.maven.repository

import org.panda_lang.reposilite.maven.metadata.MetadataUtils.toSortedBuilds
import org.panda_lang.reposilite.maven.metadata.MetadataUtils.toIdentifier
import org.panda_lang.reposilite.maven.metadata.MetadataUtils.toSortedVersions
import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.panda_lang.reposilite.config.Configuration
import org.panda_lang.reposilite.storage.StorageProviderFactory
import org.panda_lang.reposilite.maven.repository.api.RepositoryVisibility
import kotlin.Throws
import java.io.IOException
import org.panda_lang.reposilite.maven.repository.api.FileDetailsResponse
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.utilities.commons.StringUtils
import java.nio.file.Path
import java.util.*

class RepositoryService(private val journalist: Journalist) : Journalist {

    private val repositories: MutableMap<String, Repository> = LinkedHashMap(4)

    lateinit var primaryRepository: Repository
        private set

    fun load(configuration: Configuration) {
        logger.info("--- Loading repositories")
        val storageProviderFactory = StorageProviderFactory()

        for ((repositoryName, repositoryConfiguration) in configuration.repositories) {
            val repository = Repository(
                repositoryName,
                RepositoryVisibility.valueOf(repositoryConfiguration.visibility.toUpperCase()),
                storageProviderFactory.createStorageProvider(repositoryName, repositoryConfiguration.storageProvider),
                repositoryConfiguration.deployEnabled
            )

            repositories[repository.name] = repository
            val primary = primaryRepository == null

            if (primary) {
                primaryRepository = repository
            }

            logger.info("+ " + repositoryName + (if (repository.isPrivate()) " (private)" else "") + if (primary) " (primary)" else "")
        }
        logger.info(repositories.size.toString() + " repositories have been found")
    }

    fun resolveSnapshot(repository: Repository, requestPath: Path): Path? {
        val artifactFile = repository.relativize(requestPath)
        val versionDirectory = artifactFile.parent
        val builds = toSortedBuilds(repository, versionDirectory).get()
        val latestBuild = builds.firstOrNull() ?: return null
        val version = StringUtils.replace(versionDirectory.fileName.toString(), "-SNAPSHOT", StringUtils.EMPTY)
        val artifactDirectory = versionDirectory.parent
        val identifier = toIdentifier(artifactDirectory.fileName.toString(), version, latestBuild)

        return requestPath.parent.resolve(requestPath.fileName.toString().replace("SNAPSHOT", identifier))
    }

    @Throws(IOException::class)
    fun findLatest(requestedFile: Path): Optional<FileDetailsResponse> {
        if (requestedFile.fileName.toString() == "latest") {
            val parent = requestedFile.parent

            for (repository in repositories.values) {
                if (parent != null && repository.isDirectory(parent)) {
                    val files = toSortedVersions(repository, parent)
                    if (files.isOk) {
                        val latest: Path? = files.get().firstOrNull()

                        if (latest != null) {
                            return Optional.ofNullable(repository.getFileDetails(latest).orElseGet { null })
                        }
                    }
                }
            }
        }

        return Optional.empty()
    }

    fun getRepositories(token: AccessToken): Collection<Repository> {
        if (token.hasMultiaccess()) {
            return getRepositories()
        }

        for (repository in getRepositories()) {
            val name = "/" + repository.name
            if (token.getPath().startsWith(name)) {
                return listOf(repository)
            }
        }

        return emptyList()
    }

    fun getRepositories(): Collection<Repository> =
        repositories.values

    fun getRepository(repositoryName: String): Repository? =
        repositories[repositoryName]

    override fun getLogger(): Logger =
        journalist.logger

}