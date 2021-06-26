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
package org.panda_lang.reposilite.maven

import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.http.HttpCode.UNAUTHORIZED
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.failure.api.errorResponse
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.maven.api.LookupResponse
import org.panda_lang.reposilite.maven.api.Repository
import org.panda_lang.reposilite.web.orNull
import org.panda_lang.utilities.commons.function.Result
import java.nio.file.Path

internal class LookupService(
    private val repositoryService: RepositoryService,
    private val repositorySecurityProvider: RepositorySecurityProvider
) {

    fun lookup(lookupRequest: LookupRequest): Result<LookupResponse, ErrorResponse> {
        val repository = repositoryService.getRepository(lookupRequest.repository) ?: return errorResponse(NOT_FOUND, "Repository not found")
        val gav = Path.of(lookupRequest.gav)

        if (repository.isDirectory(gav) && repositorySecurityProvider.canBrowseResource(lookupRequest.accessToken, repository, gav).not()) {
            return errorResponse(UNAUTHORIZED, "Unauthorized indexing request")
        }
        else if (repositorySecurityProvider.canAccessResource(lookupRequest.accessToken, repository, gav).not()) {
            return errorResponse(UNAUTHORIZED, "Unauthorized access request")
        }

        return repository.getFileDetails(gav)
            .map { LookupResponse(it, repository.getFile(lookupRequest.gav).orNull()) }
    }

    fun findAllRepositories(): Collection<Repository> =
        repositoryService.getRepositories()

    /*
    fun exists(context: ReposiliteContext): Boolean {
        val uri: String = context.uri
        val result = repositoryAuthenticator.authDefaultRepository(context.header, uri)

        if (result.isErr) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            return if (uri.contains("-SNAPSHOT") && uri.endsWith(METADATA_FILE)) {
                false
            } else false
        }

        val path = result.get().key

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (path.nameCount < 2) {
            return false
        }

        val repository = result.get().value
        return repository.exists(path)
    }

    fun find(context: ReposiliteContext): Result<LookupResponse, ErrorResponse> {
        val uri: String = context.uri
        val result = repositoryAuthenticator.authDefaultRepository(context.header, uri)

        if (result.isErr) {
            // Maven requests maven-metadata.xml file during deploy for snapshot releases without specifying credentials
            // https://github.com/dzikoysk/reposilite/issues/184
            return if (uri.contains("-SNAPSHOT") && uri.endsWith(METADATA_FILE)) {
                ResponseUtils.error(HttpStatus.SC_NOT_FOUND, result.error.message)
            } else Result.error(result.error)
        }

        var path = result.get().key

        // discard invalid requests (less than 'group/(artifact OR metadata)')
        if (path!!.nameCount < 2) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Missing artifact identifier")
        }

        val repository = result.get().value
        val requestedFileName = path.fileName.toString()

        if (requestedFileName == "maven-metadata.xml") {
            return repository.getFile(path).map { LookupResponse.of("text/xml", Arrays.toString(it)) }
        }

        // resolve requests for latest version of artifact
        if (requestedFileName.equals("latest", ignoreCase = true)) {
            val requestDirectory = path.parent
            val versions: Result<List<Path>, ErrorResponse> = toSortedVersions(repository, requestDirectory)

            if (versions.isErr) {
                return versions.map { null }
            }

            val version = versions.get().firstOrNull()
                ?: return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Latest version not found")

            return Result.ok(LookupResponse.of("text/plain", version.fileName.toString()))
        }

        // resolve snapshot requests
        if (requestedFileName.contains("-SNAPSHOT")) {
            path = repositoryService.resolveSnapshot(repository, path)

            if (path == null) {
                return Result.error(ErrorResponse(HttpStatus.SC_NOT_FOUND, "Latest version not found"))
            }
        }

        if (repository.isDirectory(path)) {
            return ResponseUtils.error(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Directory access")
        }

        val bytes = repository.getFile(path)

        if (bytes.isErr) {
            return bytes.map { null }
        }

        val fileDetailsResult = repository.getFileDetails(path)

        return if (fileDetailsResult.isOk) {
            val fileDetails = fileDetailsResult.get()

            if (context.method != "HEAD") {
                context.output { it.write(bytes.get()) }
            }

            context.logger.debug("RESOLVED $path; mime: ${fileDetails.contentType}; size: ${repository.getFileSize(path).get()}")
            Result.ok(LookupResponse.of(fileDetails))
        }
        else {
            Result.error(fileDetailsResult.error)
        }
    }

     */
}