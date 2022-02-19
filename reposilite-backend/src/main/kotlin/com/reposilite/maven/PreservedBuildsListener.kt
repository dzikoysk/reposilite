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
import com.reposilite.storage.api.Location
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.Result.ok
import panda.std.mapToUnit
import java.lang.Integer.max

internal class PreservedBuildsListener(private val mavenFacade: MavenFacade) : EventListener<DeployEvent> {

    override fun onCall(event: DeployEvent) {
        val repository = event.repository
            .takeUnless { it.preserved < 0 }
            ?: return

        val gav = event.gav
            .takeIf { it.toString().endsWith("-SNAPSHOT/maven-metadata.xml") }
            ?: return

        val artifactDirectory = gav.locationBeforeLast("/")

        mavenFacade.findMetadata(repository.name, artifactDirectory)
            .flatMap { metadata ->
                val snapshotVersion = metadata.versioning?.snapshotVersions ?: return@flatMap ok(Unit)

                val versionsToDelete = snapshotVersion.asSequence()
                    .sortedBy { it.updated }
                    .map { it.value!! }
                    .take(max(snapshotVersion.size - repository.preserved, 0))
                    .toList()

                repository.getFiles(artifactDirectory)
                    .flatMap { deleteFiles(repository, it, versionsToDelete) }
                    .flatMap {
                        val updatedMetadata = metadata.copy(versioning = metadata.versioning.copy(
                            _snapshotVersions = snapshotVersion.filterNot { snapshot -> versionsToDelete.contains(snapshot.value) }
                        ))
                        mavenFacade.saveMetadata(repository.name, artifactDirectory, updatedMetadata)
                    }
                    .mapToUnit()
            }
            .onError { throw RuntimeException(it.toString()) } // not sure how to handle failures of events yet
    }

    private fun deleteFiles(repository: Repository, files: List<Location>, versionsToDelete: List<String>): Result<Unit, ErrorResponse> =
        files
            .filter { versionsToDelete.any { toDelete -> "$it".contains(toDelete) } }
            .forEach { repository.removeFile(it) }
            .let { ok(Unit) }

}