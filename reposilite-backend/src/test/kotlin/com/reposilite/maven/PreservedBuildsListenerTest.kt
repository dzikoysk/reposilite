package com.reposilite.maven

import com.reposilite.assertCollectionsEquals
import com.reposilite.maven.RepositoryVisibility.PUBLIC
import com.reposilite.maven.api.DeployEvent
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SnapshotVersion
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.storage.toLocation
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

        // then: builds 1 & 2 are deleted automatically
        val files = assertOk(repository.getFiles(versionId).map { it.map { file -> file.toString() }})
        assertCollectionsEquals(listOf("$prefix-20220101213702-3.jar", "$prefix-20220101213702-3.pom", "$versionId/maven-metadata.xml").map { it.toLocation().toString() }, files)

        val metadata = assertOk(mavenFacade.findMetadata(repositoryName, versionId))
        assertCollectionsEquals(listOf("1.0.0-20220101213702-3", "1.0.0-20220101213702-4"), metadata.versioning!!.snapshotVersions!!.map { it.value })
    }

}
