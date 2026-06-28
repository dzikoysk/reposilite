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

package com.reposilite.javadocs

import com.reposilite.javadocs.specification.JavadocSpecification
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.storage.api.toLocation
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import kotlin.io.path.createParentDirectories

internal class JavadocSnapshotResolutionTest : JavadocSpecification() {

    override fun repositories(): List<RepositorySettings> =
        listOf(RepositorySettings(id = "snapshots"))

    // GH-1905: in a multi-module SNAPSHOT deploy the javadoc classifier can carry a different build
    // number than the main jar. The javadoc button must resolve the javadoc's own <snapshotVersion>
    // value, not the global <snapshot> build number.
    @Test
    fun `should resolve javadoc for a snapshot whose classifier build differs from the global build`() {
        // given: metadata where the global snapshot build is -2 but the javadoc classifier only exists at -1
        val repository = "snapshots"
        val versionGav = "com/example/artifact/1.0.0-SNAPSHOT"

        addFileToRepository(FileSpec(repository, "$versionGav/maven-metadata.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.example</groupId>
              <artifactId>artifact</artifactId>
              <version>1.0.0-SNAPSHOT</version>
              <versioning>
                <snapshot>
                  <timestamp>20230817.140707</timestamp>
                  <buildNumber>2</buildNumber>
                </snapshot>
                <lastUpdated>20230817140707</lastUpdated>
                <snapshotVersions>
                  <snapshotVersion>
                    <extension>jar</extension>
                    <value>1.0.0-20230817.140707-2</value>
                    <updated>20230817140707</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>javadoc</classifier>
                    <extension>jar</extension>
                    <value>1.0.0-20230817.140707-1</value>
                    <updated>20230817140706</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
        """.trimIndent()))

        // and: the only javadoc jar on disk is the -1 build (a real jar with an index page)
        val javadocJar = workingDirectory.toPath()
            .resolve("repositories").resolve(repository)
            .resolve("$versionGav/artifact-1.0.0-20230817.140707-1-javadoc.jar".toLocation().toPath())
        javadocJar.createParentDirectories()
        writeJar(javadocJar, mapOf("index.html" to "<html></html>".toByteArray()))

        // when: the javadoc button resolves the snapshot version directory
        val result = javadocContainerService.loadContainer(
            accessToken = UNAUTHORIZED,
            repository = mavenFacade.getRepository(repository)!!,
            gav = versionGav.toLocation()
        )

        // then: it resolves the javadoc classifier's own build (-1) instead of 404ing on -2
        assertOk(result)
    }

}
