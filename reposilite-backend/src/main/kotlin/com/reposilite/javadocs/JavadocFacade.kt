/*
 * Copyright (c) 2020-2026 dzikoysk
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
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.readLines

class JavadocFacade internal constructor(
    private val journalist: Journalist,
    val mavenFacade: MavenFacade,
    private val javadocFolder: Path,
    private val javadocContainerService: JavadocContainerService
) : Journalist, Facade {

    private val supportedExtensions = mapOf(
        "html" to ContentType.TEXT_HTML,
        "css" to ContentType.TEXT_CSS,
        "js" to ContentType.TEXT_JS,
        "json" to ContentType.TEXT_JS,
        "png" to ContentType.IMAGE_PNG,
        "svg" to ContentType.IMAGE_SVG,
    )

    private data class JavadocPlainFile(
        val targetPath: Path,
        val extension: String,
        val contentType: ContentType
    )

    fun findJavadocPage(request: JavadocPageRequest): Result<JavadocResponse, ErrorResponse> =
        with (request) {
            mavenFacade.canAccessResource(accessToken, repository, gav)
                .flatMap { resolveGav(request) }
                .flatMap { resolved -> createPage(accessToken, repository, resolved) }
                .onError { logger.error("Cannot extract javadoc: ${it.message} (${it.status})}") }
        }

    fun findRawJavadocResource(request: JavadocRawRequest): Result<JavadocRawResponse, ErrorResponse> =
        with (request) {
            mavenFacade.canAccessResource(accessToken, repository, gav)
                .flatMap { javadocContainerService.loadContainer(accessToken, repository, gav) }
                .map { it.javadocUnpackPath.resolve(resource.toPath()) }
                .filter({ it.exists() }, { notFound("Resource $resource not found") })
                .map {
                    JavadocRawResponse(
                        contentType = supportedExtensions[resource.getExtension()] ?: ContentType.APPLICATION_OCTET_STREAM,
                        content = it.inputStream()
                    )
                }
        }

    private fun createPage(accessToken: AccessTokenIdentifier?, repository: Repository, gav: Location): Result<JavadocResponse, ErrorResponse> {
        val resourcesFile = createPlainFile(javadocFolder, repository, gav)

        return when {
            /* File not found */
            resourcesFile != null && resourcesFile.targetPath.notExists() ->
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
            .resolve(gav.toPath())
            .let { targetPath -> targetPath to supportedExtensions[gav.getExtension()] }
            .takeIf { (_, contentType) -> contentType != null }
            ?.let { (targetPath, contentType) -> JavadocPlainFile(targetPath, gav.getExtension(), contentType!!) }

    private fun readFile(indexFile: Path): String =
        indexFile.readLines().joinToString(separator = "\n")

    private fun resolveGav(request: JavadocPageRequest): Result<Location, ErrorResponse> {
        val segments = request.gav.toString().split('/')
        val latestIndex = segments.indexOfLast { it == "latest" }
        if (latestIndex < 0) {
            return request.gav.asSuccess()
        }

        val parent = Location.of(segments.take(latestIndex).joinToString("/"))
        val version = mavenFacade.findLatestVersion(VersionLookupRequest(request.accessToken, request.repository, parent))
            .orNull() ?: return request.gav.asSuccess()

        return parent.resolveChild(version.version)
            .map { it.resolve(segments.drop(latestIndex + 1).joinToString("/")) }
            .flatMap { gav -> mavenFacade.canAccessResource(request.accessToken, request.repository, gav).map { gav } }
    }

    override fun getLogger(): Logger =
        journalist.logger

}
