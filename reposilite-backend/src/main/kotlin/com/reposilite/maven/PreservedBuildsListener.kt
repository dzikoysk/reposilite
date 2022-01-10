package com.reposilite.maven

import com.reposilite.maven.api.DeployEvent
import com.reposilite.plugin.api.EventListener
import com.reposilite.storage.api.Location
import com.reposilite.web.http.ErrorResponse
import panda.std.Result
import panda.std.Result.ok
import panda.std.Unit
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
                val snapshotVersion = metadata.versioning?.snapshotVersions ?: return@flatMap ok()

                val versionsToDelete = snapshotVersion.asSequence()
                    .sortedBy { it.updated }
                    .map { it.value!! }
                    .take(max(snapshotVersion.size - repository.preserved, 0))
                    .toList()

                repository.getFiles(artifactDirectory)
                    .flatMap { deleteFiles(repository, it, versionsToDelete) }
                    .flatMap {
                        val updatedMetadata = metadata.copy(versioning = metadata.versioning.copy(_snapshotVersions = snapshotVersion.filterNot { snapshot -> versionsToDelete.contains(snapshot.value) }))
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
            .let { ok() }

}