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

import com.reposilite.assertCollectionsEquals
import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.Snapshot
import com.reposilite.maven.api.SnapshotVersion
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.api.toLocation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk

internal class PreservedBuildsListenerTest : MavenSpecification() {

    private lateinit var preservedBuildsListener: PreservedBuildsListener
    private val repositoryName = "snapshots"

    override fun repositories() = listOf(
        RepositorySettings(repositoryName, preserveSnapshots = false)
    )

    @BeforeEach
    fun prepareListener() {
        this.preservedBuildsListener = PreservedBuildsListener(mavenFacade)
    }

    @Test
    fun `should remove deprecated snapshot versions`() {
        // given: repository with snapshot artifact and some related files
        val artifactId = "artifact"
        val versionId = "group/$artifactId/1.0.0-R0.1-SNAPSHOT".toLocation()
        val prefix = "$versionId/$artifactId-1.0.0-R0.1"
        val newTimestamp = "20220101.213702"

        // some old artifacts
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101.213700-1.jar", "one"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101.213700-1.pom", "one"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101.213701-2.jar", "two"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101.213701-2.pom", "two"))

        // this one is currently deployed
        mavenFacade.saveMetadata(mavenFacade.getRepository(repositoryName)!!, versionId,
            Metadata(
                artifactId = artifactId,
                versioning = Versioning(
                    snapshot = Snapshot(
                        timestamp = newTimestamp
                    ),
                    _snapshotVersions = listOf(SnapshotVersion(value = "1.0.0-R0.1-$newTimestamp-3", updated = "20220101213703"))
                )
            )
        )

        // when: a new snapshot is deployed
        val repository = mavenFacade.getRepository(repositoryName)!!
        preservedBuildsListener.onCall(DeployEvent(repository, "$versionId/maven-metadata.xml".toLocation(), "junit@localhost"))

        // then: builds 1 & 2 are deleted automatically
        assertCollectionsEquals(
            setOf(
                "$versionId/maven-metadata.xml",
                "$versionId/maven-metadata.xml.md5",
                "$versionId/maven-metadata.xml.sha1",
                "$versionId/maven-metadata.xml.sha256",
                "$versionId/maven-metadata.xml.sha512"
            ).map { it.toLocation() },
            assertOk(repository.getFiles(versionId))
        )
    }

}
