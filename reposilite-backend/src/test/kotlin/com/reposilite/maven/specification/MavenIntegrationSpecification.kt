package com.reposilite.maven.specification

import com.reposilite.ReposiliteSpecification
import com.reposilite.maven.VersionComparator
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.Versioning
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile

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

    protected fun useFile(name: String, size: Long): RandomAccessFile {
        val hugeFile = RandomAccessFile(File(clientWorkingDirectory, name), "rw")
        hugeFile.setLength(size * 1024 * 1024)
        return hugeFile
    }

    protected fun useMetadata(repository: String, groupId: String, artifactId: String, versions: List<String>): Metadata {
        val sortedVersions = VersionComparator.sortStrings(versions)
        val versioning = Versioning(latest = sortedVersions.firstOrNull(), _versions = sortedVersions)
        val metadata = Metadata(groupId, artifactId, versioning = versioning)

        return reposilite.mavenFacade.saveMetadata(repository, "$groupId.$artifactId".replace(".", "/"), metadata).get()
    }

}