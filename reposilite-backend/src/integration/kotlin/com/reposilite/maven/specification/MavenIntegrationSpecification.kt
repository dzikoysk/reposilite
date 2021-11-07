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

package com.reposilite.maven.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.Versioning
import com.reposilite.shared.fs.VersionComparator
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

    protected fun useDocument(repository: String, gav: String, file: String, content: String = "test-content", store: Boolean = false): UseDocument {
        if (store) {
            reposilite.mavenFacade.deployFile(DeployRequest(repository, "$gav/$file", "junit", content.byteInputStream()))
        }

        return UseDocument(repository, gav, file, content)
    }

    protected fun useFile(name: String, sizeInMb: Int): Pair<File, Long> {
        val hugeFile = File(clientWorkingDirectory, name)
        hugeFile.writeBytes(ByteArray(sizeInMb * 1024 * 1024))
        return Pair(hugeFile, hugeFile.length())
    }

    protected fun useMetadata(repository: String, groupId: String, artifactId: String, versions: List<String>): Metadata {
        val sortedVersions = VersionComparator.sortStrings(versions)
        val versioning = Versioning(latest = sortedVersions.firstOrNull(), _versions = sortedVersions)
        val metadata = Metadata(groupId, artifactId, versioning = versioning)

        return reposilite.mavenFacade.saveMetadata(repository, "$groupId.$artifactId".replace(".", "/"), metadata).get()
    }

    protected suspend fun useProxiedHost(repository: String, gav: String, content: String, block: (String, String) -> Unit) {
        val serverStartedJob = Job()

        val application = Javalin.create()
            .events { it.serverStarted { serverStartedJob.complete() } }
            .head("/$repository/$gav") { ctx -> ctx.result(content) }
            .get("/$repository/$gav") { ctx -> ctx.result(content) }
            .start(proxiedPort)

        serverStartedJob.join()
        block(gav, content)
        application.stop()
    }

}