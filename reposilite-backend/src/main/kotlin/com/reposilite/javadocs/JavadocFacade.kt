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

package com.reposilite.javadocs

import com.reposilite.javadocs.api.JavadocPageRequest
import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.javadocs.JavadocPage.JavadocContainerPage
import com.reposilite.javadocs.JavadocPage.JavadocErrorPage
import com.reposilite.javadocs.JavadocPage.JavadocEmptyFilePage
import com.reposilite.javadocs.JavadocPage.JavadocPlainFilePage
import com.reposilite.javadocs.container.JavadocContainerService
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFound
import com.reposilite.shared.unauthorizedError
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import panda.std.Result
import java.nio.file.Files
import java.nio.file.Path

private const val LATEST_PATTERN = "/latest"
private const val VERSION_FORMAT = "/%s"

class JavadocFacade internal constructor(
    private val journalist: Journalist,
    val mavenFacade: MavenFacade,
    private val javadocFolder: Path,
    private val javadocContainerService: JavadocContainerService
) : Journalist, Facade {

    fun findJavadocPage(request: JavadocPageRequest): Result<JavadocResponse, ErrorResponse> {
        val (accessToken, repository, rawGav) = request

        if (mavenFacade.canAccessResource(accessToken, repository, rawGav).isErr) {
            return unauthorizedError()
        }

        val gav = this.resolveGav(request)
        val page = this.createPage(accessToken, repository, gav)

        return page.render()
            .onError { this.logger.error("Cannot extract javadoc: ${it.message} (${it.status})}") }
    }

    private fun createPage(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): JavadocPage {
        val repositoryFile = createPlainFile(javadocFolder, repository, gav)

        if (repositoryFile != null) {
            if (!Files.exists(repositoryFile.targetPath)) {
                return JavadocEmptyFilePage(repositoryFile)
            }

            return JavadocPlainFilePage(repositoryFile)
        }

        if (gav.contains("/resources/")) {
            return JavadocErrorPage(notFound("Resources are unavailable before extraction"))
        }

        return javadocContainerService.loadContainer(accessToken, repository, gav)
            .fold({ container -> JavadocContainerPage(container) }, { error -> JavadocErrorPage(error) })
    }

    private fun resolveGav(request: JavadocPageRequest): Location {
        if (!request.gav.contains(LATEST_PATTERN)) {
            return request.gav
        }

        val gavWithoutVersion = request.gav.locationBeforeLast(LATEST_PATTERN)

        return mavenFacade.findLatestVersion(VersionLookupRequest(request.accessToken, request.repository, gavWithoutVersion))
            .map { request.gav.replace(LATEST_PATTERN, VERSION_FORMAT.format(it.version)) }
            .orElseGet { request.gav }
    }

    override fun getLogger(): Logger = journalist.logger

}