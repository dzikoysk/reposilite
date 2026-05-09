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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.walk

internal class JavadocContainerServiceTest : JavadocSpecification() {

    @Test
    fun `should unpack a regular javadoc jar`() {
        // given: a Javadoc jar with an index page and one extra file
        val (jar, unpack) = stageJavadocJar("ok")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "Foo.html" to ByteArray(50_000) { 'a'.code.toByte() }
        ))

        // when: the jar is unpacked
        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        // then: every entry is materialized inside the unpack directory
        assertOk(result)
        assertThat(unpack.resolve("index.html").exists()).isTrue
        assertThat(unpack.resolve("index.html").readText()).isEqualTo("<html></html>")
        assertThat(unpack.resolve("Foo.html").exists()).isTrue
    }

    @Test
    fun `should reject a single entry larger than the per-entry cap`() {
        // given: a service with a 1 KiB per-entry cap and a jar with a 2 KiB entry
        val service = newJavadocContainerService(maxEntryBytes = 1024)
        val (jar, unpack) = stageJavadocJar("per-entry")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "big" to ByteArray(2048)
        ))

        // when: the jar is unpacked
        val result = service.unpackJavadocJar(jar, unpack)

        // then: extraction is rejected with a size-limit error
        assertError(result)
        assertThat(result.error.message).contains("size limit")
    }

    @Test
    fun `should reject when aggregate entry size exceeds the total cap`() {
        // given: a service with a 1 KiB per-entry cap and a 2 KiB aggregate cap, and a jar
        // whose entries individually fit but together blow the aggregate
        val service = newJavadocContainerService(maxEntryBytes = 1024, maxTotalBytes = 2048)
        val (jar, unpack) = stageJavadocJar("aggregate")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "a" to ByteArray(1024),
            "b" to ByteArray(1024),
            "c" to ByteArray(1024),
            "d" to ByteArray(1024),
        ))

        // when: the jar is unpacked
        val result = service.unpackJavadocJar(jar, unpack)

        // then: extraction is rejected with a size-limit error
        assertError(result)
        assertThat(result.error.message).contains("size limit")
    }

    @Test
    fun `should reject when entry count exceeds the cap`() {
        // given: a service with a 3-entry cap and a jar with four payload entries plus the index
        val service = newJavadocContainerService(maxEntries = 3)
        val (jar, unpack) = stageJavadocJar("count")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "a" to ByteArray(0),
            "b" to ByteArray(0),
            "c" to ByteArray(0),
            "d" to ByteArray(0),
        ))

        // when: the jar is unpacked
        val result = service.unpackJavadocJar(jar, unpack)

        // then: extraction is rejected with an entry-count error
        assertError(result)
        assertThat(result.error.message).contains("entry-count limit")
    }

    // CVE-2024-36116 / GHSA-frvj-cfq4-3228 regression check.
    // Entry names with traversal sequences must never write outside the unpack directory regardless of how Location collapses them.
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `should never escape the unpack directory regardless of entry name shape`() {
        // given: a jar whose entry names exercise traversal sequences (parent refs, leading slash,
        // backslashes, drive-letter prefix)
        val (jar, unpack) = stageJavadocJar("traversal")
        val stagingRoot = unpack.parent
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "../a.txt" to "x".toByteArray(),
            "../../b.txt" to "x".toByteArray(),
            "..\\..\\c.txt" to "x".toByteArray(),
            "/d.txt" to "x".toByteArray(),
            "C:/e.txt" to "x".toByteArray(),
        ))

        // when: the jar is unpacked
        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        // then: extraction succeeds and no file lands outside the unpack directory
        assertOk(result)
        assertThat(stagingRoot.listDirectoryEntries().map { it.fileName.toString() }).containsExactly("unpack")
        val workspace = workingDirectory.toPath()
        val unexpected = workspace.walk()
            .filter { it != workspace && !it.startsWith(stagingRoot) }
            .map { it.fileName.toString() }
            .toList()
        assertThat(unexpected).isEmpty()
    }

    @Test
    fun `should reject a jar without an index html`() {
        // given: a jar that does not contain index.html
        val (jar, unpack) = stageJavadocJar("no-index")
        writeJar(jar, mapOf("Foo.html" to "<html></html>".toByteArray()))

        // when: the jar is unpacked
        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        // then: extraction is rejected because the archive is not a valid javadoc.jar
        assertError(result)
        assertThat(result.error.message).contains("Invalid javadoc.jar")
    }

    @Test
    fun `should reject a path that does not look like a javadoc jar`() {
        // given: a sibling path that does not contain "javadoc.jar" in its filename
        val (jar, unpack) = stageJavadocJar("misnamed")
        val misnamed = jar.resolveSibling("artifact-1.0-sources.jar")
        writeJar(misnamed, mapOf("index.html" to "<html></html>".toByteArray()))

        // when: the misnamed jar is unpacked
        val result = javadocContainerService.unpackJavadocJar(misnamed, unpack)

        // then: extraction is rejected because the filename guard fired
        assertError(result)
        assertThat(result.error.message).contains("Name must contain")
    }

    @Test
    fun `should record a failure when a malformed jar is rejected`() {
        // given: a service with a 1 KiB per-entry cap and a jar with a 2 KiB entry
        val service = newJavadocContainerService(maxEntryBytes = 1024)
        val (jar, unpack) = stageJavadocJar("alert")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "big" to ByteArray(2048)
        ))
        assertThat(javadocFailureFacade.hasFailures()).isFalse

        // when: the jar is unpacked
        service.unpackJavadocJar(jar, unpack)

        // then: a Malformed-javadoc-jar failure is recorded for operator visibility
        assertThat(javadocFailureFacade.hasFailures()).isTrue
        assertThat(javadocFailureFacade.getFailures().joinToString("\n")).contains("Malformed javadoc jar")
    }

}
