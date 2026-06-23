/*
 * Copyright (c) 2026 dzikoysk
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

package com.reposilite.javadocs.specification

import com.reposilite.javadocs.JavadocContainerService
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.status.FailureFacade
import org.junit.jupiter.api.BeforeEach
import panda.std.reactive.Reference
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

internal abstract class JavadocSpecification : MavenSpecification() {

    protected lateinit var javadocContainerService: JavadocContainerService
    protected lateinit var javadocFailureFacade: FailureFacade

    override fun repositories(): List<RepositorySettings> = emptyList()

    @BeforeEach
    fun initializeJavadocService() {
        javadocFailureFacade = FailureFacade(InMemoryLogger())
        javadocContainerService = newJavadocContainerService()
    }

    protected fun newJavadocContainerService(
        maxEntryBytes: Long = 2L * 1024 * 1024,
        maxTotalBytes: Long = 50L * 1024 * 1024,
        maxEntries: Int = 50_000,
    ): JavadocContainerService =
        JavadocContainerService(
            failureFacade = javadocFailureFacade,
            mavenFacade = mavenFacade,
            javadocFolder = workingDirectory.toPath().resolve("javadocs"),
            suffixes = Reference.reference(listOf("-javadoc.jar", "-groovydoc.jar")),
            maxEntryBytes = maxEntryBytes,
            maxTotalBytes = maxTotalBytes,
            maxEntries = maxEntries,
        )

    protected fun stageJavadocJar(label: String): Pair<Path, Path> {
        val dir = workingDirectory.toPath().resolve("staging-$label").also { it.createDirectories() }
        val jar = dir.resolve("artifact-1.0-javadoc.jar")
        val unpack = dir.resolve("unpack").also { it.createDirectories() }
        return jar to unpack
    }

    protected fun writeJar(target: Path, entries: Map<String, ByteArray>) {
        ZipOutputStream(target.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }

}
