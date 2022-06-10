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

package com.reposilite.maven

import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.EventListener

internal class PreservedBuildsListener(private val mavenFacade: MavenFacade) : EventListener<DeployEvent> {

    override fun onCall(event: DeployEvent) {
        val repository = event.repository
            .takeUnless { it.preserveSnapshots }
            ?: return

        val gav = event.gav
            .takeIf { it.toString().endsWith("-SNAPSHOT/maven-metadata.xml") }
            ?: return

        val artifactDirectory = gav.locationBeforeLast("/")

        mavenFacade.findMetadata(repository.name, artifactDirectory)
            .merge(repository.getFiles(artifactDirectory)) { metadata, files -> metadata to files }
            .peek { (metadata, files) ->
                val snapshotToPreserve = metadata.versioning?.snapshot?.timestamp ?: return@peek

                files
                    .filter { it.locationAfterLast("/").toString().startsWith(metadata.artifactId!!) }
                    .filterNot { it.toString().contains(snapshotToPreserve) }
                    .map { repository.removeFile(it) }
                    .count()
                    .also { mavenFacade.logger.info("DEPLOY | Preserved Builds Listener | $it deprecated file(s) have been removed") }
            }
            .onError { throw RuntimeException(it.toString()) } // not sure how to handle failures of events yet
    }

}
