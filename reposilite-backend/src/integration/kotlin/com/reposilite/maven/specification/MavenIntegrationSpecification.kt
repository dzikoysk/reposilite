/*
 * Copyright (c) 2022 dzikoysk
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

package com.reposilite.maven.specification

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.api.Versioning
import com.reposilite.specification.ReposiliteSpecification
import com.reposilite.storage.VersionComparator
import com.reposilite.storage.api.toLocation
import io.javalin.Javalin
import kotlinx.coroutines.Job
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal data class UseDocument(
    val repository: String,
    val gav: String,
    val file: String,
    val content: String
)

internal abstract class MavenIntegrationSpecification : ReposiliteSpecification() {

    @TempDir
    lateinit var clientWorkingDirectory: File

    protected val mavenFacade by lazy { useFacade<MavenFacade>() }

    protected fun useDocument(repository: String, gav: String, file: String, content: String = "test-content", store: Boolean = false): UseDocument {
        if (store) {
            mavenFacade.deployFile(
                DeployRequest(
                    repository = mavenFacade.getRepository(repository)!!,
                    gav = "$gav/$file".toLocation(),
                    by = "junit",
                    content = content.byteInputStream()
                )
            )
        }

        return UseDocument(repository, gav, file, content)
    }

    protected fun useFile(name: String, sizeInMb: Int): Pair<File, Long> {
        val hugeFile = File(clientWorkingDirectory, name)
        hugeFile.writeBytes(ByteArray(sizeInMb * 1024 * 1024))
        return hugeFile to hugeFile.length()
    }

    protected fun useMetadata(repository: String, groupId: String, artifactId: String, versions: List<String>): Pair<String, Metadata> {
        val sortedVersions = VersionComparator.sortStrings(versions.asSequence()).toList()
        val versioning = Versioning(latest = sortedVersions.firstOrNull(), _versions = sortedVersions)
        val metadata = Metadata(groupId, artifactId, versioning = versioning)
        val mavenFacade = useFacade<MavenFacade>()

        return repository to mavenFacade.saveMetadata(
            SaveMetadataRequest(
                repository = mavenFacade.getRepository(repository)!!,
                gav = "$groupId.$artifactId".replace(".", "/").toLocation(),
                metadata = metadata
            )
        ).get()
    }

    protected suspend fun useProxiedHost(repository: String, gav: String, content: String, block: (String, String) -> Unit) {
        val serverStartedJob = Job()

        val application = Javalin.create()
            .events { it.serverStarted { serverStartedJob.complete() } }
            .head("/$repository/$gav") { ctx -> ctx.result(content) }
            .get("/$repository/$gav") { ctx -> ctx.result(content) }
            .start(reposilite.parameters.port + 1)

        serverStartedJob.join()
        block(gav, content)
        application.stop()
    }

}