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
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SnapshotVersion
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.api.toLocation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk

internal class PreservedBuildsListenerTest : MavenSpecification() {

    private lateinit var preservedBuildsListener: PreservedBuildsListener
    private val repositoryName = "snapshots"

    override fun repositories() = linkedMapOf(
        createRepository(repositoryName) {
            visibility = PUBLIC
            preserved = 2
        }
    )

    @BeforeEach
    fun prepareListener() {
        this.preservedBuildsListener = PreservedBuildsListener(mavenFacade)
    }

    @Test
    fun `should remove deprecated snapshot versions`() {
        // given: repository with snapshot artifact and some related files
        val versionId = "group/artifact/1.0.0-SNAPSHOT".toLocation()

        mavenFacade.saveMetadata(repositoryName, versionId, Metadata(versioning = Versioning(_snapshotVersions = listOf(
            SnapshotVersion(value = "1.0.0-20220101213700-1", updated = "20220101213700"),
            SnapshotVersion(value = "1.0.0-20220101213701-2", updated = "20220101213701"),
            SnapshotVersion(value = "1.0.0-20220101213702-3", updated = "20220101213702"),
            SnapshotVersion(value = "1.0.0-20220101213702-4", updated = "20220101213703") // this one is currently deployed, so we don't need its files
        ))))

        val prefix = "$versionId/artifact-1.0.0"

        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213700-1.jar", "one"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213700-1.pom", "one"))

        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213701-2.jar", "two"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213701-2.pom", "two"))

        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213702-3.jar", "three"))
        addFileToRepository(FileSpec(repositoryName, "$prefix-20220101213702-3.pom", "three"))

        // when: a new snapshot is deployed
        val repository = mavenFacade.getRepository(repositoryName)!!
        preservedBuildsListener.onCall(DeployEvent(repository, "$versionId/maven-metadata.xml".toLocation(), "junit"))

        // then: builds 1 & 2 are deleted automatically, the metadata file should be updated with checksums
        val files = assertOk(repository.getFiles(versionId).map { it.map { file -> file.toString() }})
        assertCollectionsEquals(listOf(
            "$prefix-20220101213702-3.jar",
            "$prefix-20220101213702-3.pom",
            "$versionId/maven-metadata.xml",
            "$versionId/maven-metadata.xml.md5",
            "$versionId/maven-metadata.xml.sha1",
            "$versionId/maven-metadata.xml.sha256",
            "$versionId/maven-metadata.xml.sha512"
        ).map { it.toLocation().toString() }, files)

        val metadata = assertOk(mavenFacade.findMetadata(repositoryName, versionId))
        assertCollectionsEquals(listOf("1.0.0-20220101213702-3", "1.0.0-20220101213702-4"), metadata.versioning!!.snapshotVersions!!.map { it.value })
    }

}
