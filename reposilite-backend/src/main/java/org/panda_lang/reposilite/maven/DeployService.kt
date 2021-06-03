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

import net.dzikoysk.dynamiclogger.Journalist
import net.dzikoysk.dynamiclogger.Logger
import org.apache.http.HttpStatus
import org.panda_lang.reposilite.failure.ResponseUtils
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.api.DeployRequest
import org.panda_lang.reposilite.maven.api.FileDetailsResponse
import org.panda_lang.utilities.commons.function.Result
import java.nio.file.Paths

internal class DeployService(
    private val journalist: Journalist,
    private val rewritePathsEnabled: Boolean,
    private val repositoryService: RepositoryService,
    private val metadataService: MetadataService
) : Journalist {

    fun deployArtifact(deployRequest: DeployRequest): Result<FileDetailsResponse, ErrorResponse> {
        val repository = repositoryService.getRepository(deployRequest.repository) ?: return ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Repository not found")

        if (!repository.isDeployEnabled) {
            return ResponseUtils.error(HttpStatus.SC_METHOD_NOT_ALLOWED, "Artifact deployment is disabled")
        }

        if (repository.isFull()) {
            return ResponseUtils.error(HttpStatus.SC_INSUFFICIENT_STORAGE, "Not enough storage space available")
        }

        repository.relativize(deployRequest.gav)
        val path = Paths.get(deployRequest.path)
        val metadataFile = path.resolveSibling("maven-metadata.xml")
        metadataService.clearMetadata(metadataFile)

        return try {
            val result: Result<FileDetailsResponse, ErrorResponse> =
                if (path.fileName.toString().contains("maven-metadata")) {
                    metadataService.getMetadata(repository, metadataFile).map { it.key }
                }
                else {
                    repository.putFile(path, deployRequest.content)
                }

            result.peek { logger.info("DEPLOY Artifact successfully deployed $path by ${deployRequest.by}") }
        }
        catch (exception: Exception) {
            Result.error(ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to upload artifact"))
        }
    }

    override fun getLogger(): Logger =
        journalist.logger

}