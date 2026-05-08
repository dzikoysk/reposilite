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
        val (jar, unpack) = stageJavadocJar("ok")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "Foo.html" to ByteArray(50_000) { 'a'.code.toByte() }
        ))

        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        assertOk(result)
        assertThat(unpack.resolve("index.html").exists()).isTrue
        assertThat(unpack.resolve("index.html").readText()).isEqualTo("<html></html>")
        assertThat(unpack.resolve("Foo.html").exists()).isTrue
    }

    @Test
    fun `should reject a single entry larger than the per-entry cap (decompression bomb)`() {
        val service = newJavadocContainerService(maxEntryBytes = 1024)
        val (jar, unpack) = stageJavadocJar("per-entry")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "bomb" to ByteArray(2048)  // > 1 KiB per-entry cap
        ))

        val result = service.unpackJavadocJar(jar, unpack)

        assertError(result)
        assertThat(result.error.message).contains("size limit")
    }

    @Test
    fun `should reject when aggregate entry size exceeds the total cap`() {
        // each entry within the 1 KiB per-entry cap, but five of them blow the 2 KiB aggregate
        val service = newJavadocContainerService(maxEntryBytes = 1024, maxTotalBytes = 2048)
        val (jar, unpack) = stageJavadocJar("aggregate")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "a" to ByteArray(1024),
            "b" to ByteArray(1024),
            "c" to ByteArray(1024),
            "d" to ByteArray(1024),
        ))

        val result = service.unpackJavadocJar(jar, unpack)

        assertError(result)
        assertThat(result.error.message).contains("size limit")
    }

    @Test
    fun `should reject when entry count exceeds the cap`() {
        val service = newJavadocContainerService(maxEntries = 3)
        val (jar, unpack) = stageJavadocJar("count")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "a" to ByteArray(0),
            "b" to ByteArray(0),
            "c" to ByteArray(0),
            "d" to ByteArray(0),
        ))

        val result = service.unpackJavadocJar(jar, unpack)

        assertError(result)
        assertThat(result.error.message).contains("entry-count limit")
    }

    // CVE-2024-36116 / GHSA-frvj-cfq4-3228 regression check.
    // Entry names with traversal sequences must never write outside the unpack directory regardless of how Location collapses them.
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `should never escape the unpack directory regardless of malicious entry names`() {
        val (jar, unpack) = stageJavadocJar("traversal")
        val stagingRoot = unpack.parent
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "../escape-up.txt" to "pwned".toByteArray(),
            "../../escape-up-twice.txt" to "pwned".toByteArray(),
            "..\\..\\escape-windows.txt" to "pwned".toByteArray(),
            "/absolute-escape.txt" to "pwned".toByteArray(),
            "C:/drive-escape.txt" to "pwned".toByteArray(),
        ))

        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        assertOk(result)
        // staging-traversal/ should now contain only unpack/ — the jar is deleted post-unpack and no
        // siblings should have been created by traversal payloads
        assertThat(stagingRoot.listDirectoryEntries().map { it.fileName.toString() }).containsExactly("unpack")
        // and nothing should exist above staging-traversal/ either
        val workspace = workingDirectory.toPath()
        val unexpected = workspace.walk()
            .filter { it != workspace && !it.startsWith(stagingRoot) }
            .map { it.fileName.toString() }
            .toList()
        assertThat(unexpected).isEmpty()
    }

    @Test
    fun `should reject a jar without an index html`() {
        val (jar, unpack) = stageJavadocJar("no-index")
        writeJar(jar, mapOf("Foo.html" to "<html></html>".toByteArray()))

        val result = javadocContainerService.unpackJavadocJar(jar, unpack)

        assertError(result)
        assertThat(result.error.message).contains("Invalid javadoc.jar")
    }

    @Test
    fun `should reject a path that does not look like a javadoc jar`() {
        val (jar, unpack) = stageJavadocJar("misnamed")
        // stageJavadocJar produces "artifact-1.0-javadoc.jar" — point at a sibling with a non-matching name
        val misnamed = jar.resolveSibling("artifact-1.0-sources.jar")
        writeJar(misnamed, mapOf("index.html" to "<html></html>".toByteArray()))

        val result = javadocContainerService.unpackJavadocJar(misnamed, unpack)

        assertError(result)
        assertThat(result.error.message).contains("Name must contain")
    }

    @Test
    fun `should record a failure when a malformed jar is rejected`() {
        val service = newJavadocContainerService(maxEntryBytes = 1024)
        val (jar, unpack) = stageJavadocJar("alert")
        writeJar(jar, mapOf(
            "index.html" to "<html></html>".toByteArray(),
            "bomb" to ByteArray(2048)
        ))

        assertThat(javadocFailureFacade.hasFailures()).isFalse
        service.unpackJavadocJar(jar, unpack)
        assertThat(javadocFailureFacade.hasFailures()).isTrue
        assertThat(javadocFailureFacade.getFailures().joinToString("\n")).contains("Malformed javadoc jar")
    }

}
