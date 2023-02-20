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
import com.reposilite.javadocs.api.JavadocRawRequest
import com.reposilite.javadocs.api.JavadocRawResponse
import com.reposilite.javadocs.api.JavadocResponse
import com.reposilite.journalist.Journalist
import com.reposilite.journalist.Logger
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.notFound
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.Location
import com.reposilite.token.AccessTokenIdentifier
import io.javalin.http.ContentType
import panda.std.Result
import panda.std.Result.supplyThrowing
import panda.std.asSuccess
import panda.utilities.StringUtils
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

private const val LATEST_PATTERN = "/latest"

class JavadocFacade internal constructor(
    private val journalist: Journalist,
    val mavenFacade: MavenFacade,
    private val javadocFolder: Path,
    private val javadocContainerService: JavadocContainerService
) : Journalist, Facade {

    private val supportedExtensions = mapOf(
        "html" to ContentType.TEXT_HTML,
        "css" to ContentType.TEXT_CSS,
        "js" to ContentType.APPLICATION_JS,
        "png" to ContentType.IMAGE_PNG,
    )

    private data class JavadocPlainFile(
        val targetPath: Path,
        val extension: String,
        val contentType: ContentType
    )

    fun findJavadocPage(request: JavadocPageRequest): Result<JavadocResponse, ErrorResponse> =
        with (request) {
            mavenFacade.canAccessResource(accessToken, repository, gav)
                .flatMap { createPage(accessToken, repository, resolveGav(request)) }
                .onError { logger.error("Cannot extract javadoc: ${it.message} (${it.status})}") }
        }

    fun findRawJavadocResource(request: JavadocRawRequest): Result<JavadocRawResponse, ErrorResponse> =
        with (request) {
            mavenFacade.canAccessResource(accessToken, repository, gav)
                .flatMap { javadocContainerService.loadContainer(accessToken, repository, gav) }
                .map {
                    JavadocRawResponse(
                        contentType = supportedExtensions[resource.getExtension()] ?: ContentType.APPLICATION_OCTET_STREAM,
                        content = Files.newInputStream(it.javadocUnpackPath.resolve(resource.toString()))
                    )
                }
        }

    private fun createPage(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocResponse, ErrorResponse> {
        val resourcesFile = createPlainFile(javadocFolder, repository, gav)

        return when {
            /* File not found */
            resourcesFile != null && !Files.exists(resourcesFile.targetPath) ->
                JavadocResponse(resourcesFile.contentType.mimeType, StringUtils.EMPTY).asSuccess()
            /* File exists */
            resourcesFile != null ->
                supplyThrowing {
                    JavadocResponse(resourcesFile.contentType.mimeType, readFile(resourcesFile.targetPath))
                }.mapErr {
                    notFound("Resource not found!")
                }
            /* Premature resources request */
            gav.contains("/resources/") ->
                notFoundError("Resources are unavailable before extraction")
            /* Load resource */
            else ->
                javadocContainerService
                    .loadContainer(accessToken, repository, gav)
                    .map { JavadocResponse(ContentType.HTML, readFile(it.javadocContainerIndex)) }
        }
    }

    private fun createPlainFile(javadocFolder: Path, repository: Repository, gav: Location): JavadocPlainFile? =
        javadocFolder
            .resolve(repository.name)
            .resolve(gav.toString())
            .let { targetPath -> targetPath to supportedExtensions[gav.getExtension()] }
            .takeIf { (_, contentType) -> contentType != null }
            ?.let { (targetPath, contentType) -> JavadocPlainFile(targetPath, gav.getExtension(), contentType!!) }

    private fun readFile(indexFile: Path): String =
        Files.readAllLines(indexFile).joinToString(separator = "\n")

    private fun resolveGav(request: JavadocPageRequest): Location =
        request.gav
            .takeIf { it.contains("/latest") }
            ?.let { request.gav.locationBeforeLast("/latest") }
            ?.let { gavWithoutVersion -> VersionLookupRequest(request.accessToken, request.repository, gavWithoutVersion) }
            ?.let { mavenFacade.findLatestVersion(it) }
            ?.map { request.gav.replace(LATEST_PATTERN, "/%s".format(it.version)) }
            ?.orNull()
            ?: request.gav

    override fun getLogger(): Logger =
        journalist.logger

}
