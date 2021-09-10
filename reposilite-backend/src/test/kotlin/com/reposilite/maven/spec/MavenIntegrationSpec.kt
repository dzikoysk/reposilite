package com.reposilite.maven.spec

import com.reposilite.ReposiliteSpec
import com.reposilite.maven.api.DeployRequest
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile

internal abstract class MavenIntegrationSpec : ReposiliteSpec() {

    @TempDir
    lateinit var clientWorkingDirectory: File

    data class UseDocument(
        val repository: String,
        val gav: String,
        val file: String,
        val content: String
    )

    protected fun useDocument(repository: String, gav: String, file: String, content: String = "", store: Boolean = false): UseDocument {
        if (store) {
            reposilite.mavenFacade.deployFile(DeployRequest(repository, "$gav/$file", "unknown", content.byteInputStream()))
        }

        return UseDocument(repository, gav, file, content)
    }

    protected fun useFile(name: String, size: Long): RandomAccessFile {
        val hugeFile = RandomAccessFile(File(clientWorkingDirectory, name), "rw")
        hugeFile.setLength(size * 1024 * 1024)
        return hugeFile
    }

}