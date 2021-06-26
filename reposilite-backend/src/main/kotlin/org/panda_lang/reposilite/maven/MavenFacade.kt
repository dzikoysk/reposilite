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
import org.panda_lang.reposilite.maven.api.FileDetails
import org.panda_lang.reposilite.maven.api.LookupRequest
import org.panda_lang.reposilite.maven.api.LookupResponse
import org.panda_lang.reposilite.maven.api.Repository
import org.panda_lang.utilities.commons.function.Result

class MavenFacade internal constructor(
    private val journalist: Journalist,
    private val metadataService: MetadataService,
    private val lookupService: LookupService,
    private val deploymentService: DeploymentService,
) : Journalist {

    fun lookup(lookupRequest: LookupRequest): Result<LookupResponse, ErrorResponse> =
        lookupService.lookup(lookupRequest)

    fun deployArtifact(deployRequest: DeployRequest): Result<FileDetails, ErrorResponse> =
        deploymentService.deployArtifact(deployRequest)

    fun getRepositories(): Collection<Repository> =
        lookupService.findAllRepositories()

    override fun getLogger(): Logger =
        journalist.logger

}