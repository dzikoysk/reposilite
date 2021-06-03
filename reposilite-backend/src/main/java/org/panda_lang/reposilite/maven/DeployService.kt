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

import org.apache.http.HttpStatus
import org.panda_lang.reposilite.web.ReposiliteUtils.normalizeUri
import org.panda_lang.reposilite.web.ReposiliteUtils.getRepository
import org.panda_lang.reposilite.web.ReposiliteContext
import org.panda_lang.reposilite.maven.api.FileDetailsResponse
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.auth.Authenticator
import org.panda_lang.reposilite.failure.ResponseUtils
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.RoutePermission.WRITE
import org.panda_lang.utilities.commons.function.Result
import java.lang.Exception
import java.nio.file.Paths

internal class DeployService(
    private val rewritePathsEnabled: Boolean,
    private val authenticator: Authenticator,
    private val repositoryService: RepositoryService,
    private val metadataService: MetadataService
) {

    fun deploy(context: ReposiliteContext): Result<FileDetailsResponse, ErrorResponse> {
        val uriValue = normalizeUri(context.uri)

        if (uriValue.isEmpty) {
            return ResponseUtils.error(HttpStatus.SC_BAD_REQUEST, "Invalid GAV path")
        }

        val uri = uriValue.get()
        val authResult: Result<AccessToken, String> = authenticator.authByUri(context.header, uri)

        if (authResult.isErr) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, authResult.error)
        }

        val session = authResult.get()

        if (!session.hasPermission(WRITE) && !session.isManager()) {
            return ResponseUtils.error(HttpStatus.SC_UNAUTHORIZED, "Cannot deploy artifact without write permission")
        }

        val repositoryValue = getRepository(
            rewritePathsEnabled, repositoryService, uri
        )

        if (repositoryValue.isEmpty) {
            return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Repository not found")
        }

        val repository = repositoryValue.get()

        if (!repository.isDeployEnabled) {
            return ResponseUtils.error(HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled")
        }

        if (repository.isFull()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available")
        }

        val path = Paths.get(uri)
        val metadataFile = path.resolveSibling("maven-metadata.xml")
        metadataService.clearMetadata(metadataFile)

        return try {
            val result: Result<FileDetailsResponse, ErrorResponse> =
                if (path.fileName.toString().contains("maven-metadata")) {
                    metadataService.getMetadata(repository, metadataFile).map { it.key }
                }
                else {
                    repository.putFile(path, context.input())
                }

            if (result.isOk) {
                context.logger.info("DEPLOY " + authResult.isOk + " successfully deployed " + path + " from " + context.address())
            }

            result
        }
        catch (exception: Exception) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact"))
        }
    }

}