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
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.maven.api.DeployRequest
import org.panda_lang.reposilite.maven.api.FileDetailsResponse
import org.panda_lang.utilities.commons.function.Result

class MavenFacade internal constructor(
    internal val journalist: Journalist,
    internal val repositoryService: RepositoryService,
    internal val metadataService: MetadataService,
    internal val deployService: DeployService,
) : Journalist {

    fun deployArtifact(deployRequest: DeployRequest): Result<FileDetailsResponse, ErrorResponse> =
        deployService.deployArtifact(deployRequest)

    fun getRepositories(): Collection<Repository> =
        repositoryService.getRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}