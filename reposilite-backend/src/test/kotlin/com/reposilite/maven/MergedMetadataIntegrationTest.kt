/*
 * Copyright (c) 2020-2026 dzikoysk
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

import com.fasterxml.jackson.module.kotlin.readValue
import com.reposilite.frontend.FrontendFacade
import com.reposilite.frontend.application.FrontendSettings
import com.reposilite.journalist.backend.InMemoryLogger
import com.reposilite.maven.api.Checksum
import com.reposilite.maven.api.DeleteRequest
import com.reposilite.maven.api.DeployRequest
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.METADATA_FILE
import com.reposilite.maven.api.Metadata
import com.reposilite.maven.api.SaveMetadataRequest
import com.reposilite.maven.api.VersionLookupRequest
import com.reposilite.maven.api.Versioning
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.infrastructure.MavenEndpoints
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.repository.api.RepositoryVisibility.PUBLIC
import com.reposilite.shared.errorResponse
import com.reposilite.shared.http.FakeRemoteClientProvider
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.shared.notFoundError
import com.reposilite.status.application.FailureComponents
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.toLocation
import com.reposilite.token.RoutePermission.READ
import com.reposilite.token.RoutePermission.WRITE
import io.javalin.Javalin
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.BAD_GATEWAY
import io.javalin.http.HttpStatus.SERVICE_UNAVAILABLE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import panda.std.ResultAssertions.assertError
import panda.std.ResultAssertions.assertOk
import panda.std.asSuccess
import panda.std.reactive.reference
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class MergedMetadataIntegrationTest : MavenSpecification() {

    private companion object {
        const val CENTRAL = "https://central.example/maven2"
        const val BROKEN = "https://broken.example/maven2"
        const val OTHER = "https://other.example/maven2"
        const val GAV = "foo/bar"
        const val METADATA = "$GAV/$METADATA_FILE"

        val RELEASES_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <versioning>
                <latest>4.0.0</latest>
                <release>4.0.0</release>
                <versions>
                  <version>3.0.0</version>
                  <version>4.0.0</version>
                </versions>
                <lastUpdated>20260216073143</lastUpdated>
              </versioning>
            </metadata>
        """.trimIndent()

        val SNAPSHOTS_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <versioning>
                <latest>4.1.0-SNAPSHOT</latest>
                <versions>
                  <version>3.0.0-SNAPSHOT</version>
                  <version>4.0.0-SNAPSHOT</version>
                  <version>4.1.0-SNAPSHOT</version>
                </versions>
                <lastUpdated>20260219082051</lastUpdated>
              </versioning>
            </metadata>
        """.trimIndent()

        val CENTRAL_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <versioning>
                <latest>1.0.0</latest>
                <release>1.0.0</release>
                <versions>
                  <version>1.0.0</version>
                </versions>
                <lastUpdated>20200101000000</lastUpdated>
              </versioning>
            </metadata>
        """.trimIndent()
    }

    private val remoteFiles = ConcurrentHashMap<String, String>()
    private val remoteRequests = ConcurrentHashMap<String, AtomicInteger>()
    private val remoteFailures = ConcurrentHashMap<String, HttpStatus>()
    private var afterRemoteGet: (String) -> Unit = {}
    @Volatile private var beforeClockRead: () -> Unit = {}
    private var now = Instant.parse("2026-10-30T10:00:00Z")
    override val clock: Clock = object : Clock() {
        override fun instant(): Instant {
            beforeClockRead()
            return now
        }
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }

    override val remoteClientProviderOverride: RemoteClientProvider = FakeRemoteClientProvider(
        headHandler = { uri, _, _, _ ->
            remoteFiles[uri]
                ?.let { DocumentInfo(name = uri.substringAfterLast('/'), contentType = APPLICATION_OCTET_STREAM, contentLength = it.length.toLong()).asSuccess() }
                ?: notFoundError("Not found")
        },
        getHandler = { uri, _, _, _ ->
            remoteRequests.computeIfAbsent(uri) { AtomicInteger() }.incrementAndGet()
            when {
                uri.startsWith(BROKEN) -> errorResponse(BAD_GATEWAY, "Simulated upstream failure")
                remoteFailures.keys.any { uri.startsWith(it) } -> errorResponse(remoteFailures.entries.first { uri.startsWith(it.key) }.value, "Simulated upstream failure")
                else -> {
                    val content = remoteFiles[uri]
                    afterRemoteGet(uri)
                    content?.byteInputStream()?.asSuccess() ?: notFoundError("Not found")
                }
            }
        }
    )

    override fun repositories() = listOf(
        RepositorySettings(id = "foo-releases", visibility = PUBLIC),
        RepositorySettings(id = "foo-snapshots", visibility = PUBLIC),
        RepositorySettings(
            id = "foo-all",
            visibility = PUBLIC,
            mergeMetadata = true,
            proxied = listOf(
                MirroredRepositorySettings(reference = "foo-releases"),
                MirroredRepositorySettings(reference = "foo-snapshots"),
                MirroredRepositorySettings(reference = CENTRAL, store = true),
            ),
        ),
        RepositorySettings(
            id = "foo-first-match",
            visibility = PUBLIC,
            proxied = listOf(
                MirroredRepositorySettings(reference = "foo-releases"),
                MirroredRepositorySettings(reference = "foo-snapshots"),
            ),
        ),
        RepositorySettings(
            id = "remote-cached",
            visibility = PUBLIC,
            mergeMetadata = true,
            metadataMaxAge = 60,
            proxied = listOf(
                MirroredRepositorySettings(reference = CENTRAL),
                MirroredRepositorySettings(reference = BROKEN),
            ),
        ),
        RepositorySettings(
            id = "foo-cached",
            visibility = PUBLIC,
            mergeMetadata = true,
            metadataMaxAge = 60,
            proxied = listOf(MirroredRepositorySettings(reference = "foo-releases")),
        ),
        RepositorySettings(
            id = "remote-uncached",
            visibility = PUBLIC,
            mergeMetadata = true,
            proxied = listOf(MirroredRepositorySettings(reference = CENTRAL)),
        ),
        RepositorySettings(
            id = "remote-stored",
            visibility = PUBLIC,
            proxied = listOf(MirroredRepositorySettings(reference = CENTRAL, store = true)),
        ),
        RepositorySettings(
            id = "central-cached",
            visibility = PUBLIC,
            mergeMetadata = true,
            metadataMaxAge = 60,
            proxied = listOf(MirroredRepositorySettings(reference = CENTRAL)),
        ),
        RepositorySettings(
            id = "mixed",
            visibility = PUBLIC,
            mergeMetadata = true,
            proxied = listOf(
                MirroredRepositorySettings(reference = "foo-releases"),
                MirroredRepositorySettings(reference = CENTRAL),
                MirroredRepositorySettings(reference = OTHER),
            ),
        ),
        RepositorySettings(
            id = "remote-broken",
            visibility = PUBLIC,
            mergeMetadata = true,
            proxied = listOf(MirroredRepositorySettings(reference = BROKEN)),
        ),
    )

    private fun publishReleasesAndSnapshots() {
        addFileToRepository(FileSpec("foo-releases", METADATA, RELEASES_METADATA))
        addFileToRepository(FileSpec("foo-snapshots", METADATA, SNAPSHOTS_METADATA))
    }

    /** The local metadata file only contributes versions that are stored in the repository. */
    private fun storeVersions(repository: String, vararg versions: String) {
        versions.forEach { addFileToRepository(FileSpec(repository, "$GAV/$it/bar-$it.pom", "pom")) }
    }

    private fun readMetadata(repository: String, gav: String = METADATA): Metadata =
        assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, repository, gav.toLocation())))
            .use { metadataXmlMapper.readValue(it) }

    @Test
    fun `should reject well formed HTML instead of returning empty metadata`() {
        remoteFiles["$CENTRAL/$METADATA"] = "<html><head><title>Sign in</title></head><body>Authentication required</body></html>"

        val error = assertError(mavenFacade.findData(LookupRequest(null, "central-cached", METADATA.toLocation())))

        assertThat(error.status).isEqualTo(404)
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        assertThat(readMetadata("central-cached").versioning?.versions).containsExactly("1.0.0")
    }

    @Test
    fun `should preserve upstream errors when another mirror serves foreign XML`() {
        remoteFiles["$CENTRAL/$METADATA"] = "<error><message>Access denied</message></error>"

        val error = assertError(mavenFacade.findData(LookupRequest(null, "remote-cached", METADATA.toLocation())))

        assertThat(error.status).isEqualTo(502)
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0")
    }

    @Test
    fun `should skip foreign XML and still merge valid local sources`() {
        publishReleasesAndSnapshots()
        remoteFiles["$CENTRAL/$METADATA"] = "<html><body>Sign in</body></html>"

        assertThat(readMetadata("foo-all").versioning?.versions)
            .containsExactly("3.0.0-SNAPSHOT", "3.0.0", "4.0.0-SNAPSHOT", "4.0.0", "4.1.0-SNAPSHOT")
    }

    @ParameterizedTest
    @ValueSource(strings = ["<groupId>other</groupId>", "<groupId>foo.bar</groupId>", "<artifactId>baz</artifactId>", "<version>2.0-SNAPSHOT</version>"])
    fun `should skip mirrors that declare different coordinates`(coordinate: String) {
        remoteFiles["$CENTRAL/$METADATA"] =
            "<metadata>$coordinate<versioning><versions><version>9.0.0</version></versions></versioning></metadata>"
        remoteFiles["$OTHER/$METADATA"] = CENTRAL_METADATA

        val metadata = readMetadata("mixed")
        assertThat(metadata.groupId).isEqualTo("foo")
        assertThat(metadata.artifactId).isEqualTo("bar")
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0")
    }

    @Test
    fun `should accept artifact metadata that declares a version`() {
        // given: metadata of an artifact directory as written by older clients and for Gradle plugin markers
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA.replace("</artifactId>", "</artifactId><version>1.0.0</version>")
        remoteFiles["$OTHER/$METADATA"] = CENTRAL_METADATA.replace("1.0.0", "2.0.0").replace("</artifactId>", "</artifactId><version>2.0.0</version>")

        // then: the declared version does not turn it into metadata of a version directory
        assertThat(readMetadata("mixed").versioning?.versions).containsExactly("1.0.0", "2.0.0")
    }

    @Test
    fun `should match plugin metadata at the group path`() {
        val path = "org/example/$METADATA_FILE"
        val plugins = "<plugins><plugin><prefix>a</prefix><artifactId>a-plugin</artifactId></plugin></plugins>"
        remoteFiles["$CENTRAL/$path"] = "<metadata><groupId>org.example</groupId>$plugins</metadata>"
        remoteFiles["$OTHER/$path"] = "<metadata><groupId>org</groupId>${plugins.replace("a-plugin", "wrong-plugin")}</metadata>"
        assertThat(readMetadata("mixed", path).plugins?.map { it.artifactId }).containsExactly("a-plugin")
    }

    @Test
    fun `should serve explicit local and mirrored versions through the versions API`() {
        mavenSettings.update(mavenSettings.get().copy(repositories = repositories().map {
            if (it.id == "central-cached") it.copy(proxied = listOf(MirroredRepositorySettings(reference = CENTRAL, authenticatedFetchingOnly = true))) else it
        }))
        val repository = mavenFacade.getRepository("central-cached")!!
        storeVersions(repository.name, "2.0.0", "2.1.0-SNAPSHOT")
        addFileToRepository(FileSpec(repository.name, METADATA, """
            <metadata><groupId>foo</groupId><artifactId>bar</artifactId>
            <versioning><release>2.0.0</release><latest>2.1.0-SNAPSHOT</latest></versioning></metadata>
        """.trimIndent()))
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        val token = createAccessToken("reader", "secret", repository.name, GAV, READ)
        val request = VersionLookupRequest(token, repository, GAV.toLocation())

        val merged = assertOk(mavenFacade.findData(LookupRequest(token, repository.name, METADATA.toLocation())))
            .use { metadataXmlMapper.readValue<Metadata>(it) }
        assertThat(merged.versioning?.versions).containsExactly("1.0.0", "2.0.0", "2.1.0-SNAPSHOT")
        assertThat(merged.versioning?.latest).isEqualTo("2.1.0-SNAPSHOT")
        assertThat(merged.versioning?.release).isEqualTo("2.0.0")
        assertThat(assertOk(mavenFacade.findVersions(request)).versions).isEqualTo(merged.versioning?.versions)
        assertThat(assertOk(mavenFacade.findLatestVersion(request)).version).isEqualTo(merged.versioning?.latest)
        assertThat(assertOk(mavenFacade.findLatestVersion(request.copy(filter = "1."))).version).isEqualTo("1.0.0")
        assertThat(assertOk(mavenFacade.findVersions(request.copy(accessToken = null))).versions).containsExactly("2.0.0", "2.1.0-SNAPSHOT")
    }

    @Test
    fun `should accept metadata with an XML declaration comment and namespace`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA.replace(
            "<metadata>", "<!-- Maven metadata -->\n<metadata xmlns=\"http://maven.apache.org/METADATA/1.1.0\">",
        )

        assertThat(readMetadata("remote-uncached").versioning?.versions).containsExactly("1.0.0")
    }

    @Test
    fun `should resolve details and content from one uncached merge`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        afterRemoteGet = { remoteFiles[it] = RELEASES_METADATA }

        val file = assertOk(mavenFacade.findFile(LookupRequest(null, "remote-uncached", METADATA.toLocation())))
        val content = file.content.use { it.readBytes() }

        assertThat(file.document.contentLength).isEqualTo(content.size.toLong())
        assertThat(metadataXmlMapper.readValue<Metadata>(content).versioning?.versions).containsExactly("1.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
    }

    @Test
    fun `should serve HTTP headers and body from one uncached merge`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        afterRemoteGet = { remoteFiles[it] = RELEASES_METADATA }
        val endpoints = MavenEndpoints(
            mavenFacade,
            FrontendFacade(reference("/"), reference(FrontendSettings()), reference("X-Forwarded-Prefix")),
            "none",
        )
        val server = Javalin.create { config ->
            config.routes.get("/metadata") { endpoints.findFile(it, null, "remote-uncached", METADATA.toLocation()) }
            config.routes.head("/metadata") { endpoints.findFile(it, null, "remote-uncached", METADATA.toLocation()) }
        }.start(0)

        try {
            HttpClient.newHttpClient().let { client ->
                val request = HttpRequest.newBuilder(URI("http://127.0.0.1:${server.port()}/metadata"))
                    .timeout(Duration.ofSeconds(10))
                val response = client.send(request.GET().build(), BodyHandlers.ofByteArray())

                assertThat(response.statusCode()).isEqualTo(200)
                assertThat(response.headers().firstValueAsLong("Content-Length").asLong).isEqualTo(response.body().size.toLong())
                assertThat(metadataXmlMapper.readValue<Metadata>(response.body()).versioning?.versions).containsExactly("1.0.0")
                assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)

                val head = client.send(request.method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), BodyHandlers.ofByteArray())
                assertThat(head.statusCode()).isEqualTo(200)
                assertThat(head.body()).isEmpty()
                assertThat(head.headers().firstValueAsLong("Content-Length").asLong).isGreaterThan(0)
                assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `should invalidate cached metadata saved through metadata service`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        storeVersions("remote-cached", "3.0.0")
        readMetadata("remote-cached")

        assertOk(mavenFacade.saveMetadata(SaveMetadataRequest(
            mavenFacade.getRepository("remote-cached")!!,
            GAV.toLocation(),
            Metadata(groupId = "foo", artifactId = "bar", versioning = Versioning(_versions = listOf("3.0.0"))),
        )))

        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0", "3.0.0")
    }

    @Test
    fun `should invalidate metadata below deleted directories in other repositories`() {
        publishReleasesAndSnapshots()
        readMetadata("foo-cached")
        val token = createAccessToken("delete", "secret", "foo-releases", "foo", WRITE)

        assertOk(mavenFacade.deleteFile(DeleteRequest(token, mavenFacade.getRepository("foo-releases")!!, GAV.toLocation(), "test")))

        val error = assertError(mavenFacade.findData(LookupRequest(null, "foo-cached", METADATA.toLocation())))
        assertThat(error.status).isEqualTo(404)
    }

    @Test
    fun `should discard merged results after mirror settings change`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        readMetadata("remote-cached")

        mavenSettings.update(mavenSettings.get().copy(repositories = repositories().map { settings ->
            if (settings.id == "remote-cached") settings.copy(proxied = listOf(
                MirroredRepositorySettings(reference = CENTRAL, authenticatedFetchingOnly = true)
            )) else settings
        }))

        val error = assertError(mavenFacade.findData(LookupRequest(null, "remote-cached", METADATA.toLocation())))
        assertThat(error.status).isEqualTo(404)
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
    }

    @Test
    fun `should expire cached merges without extending their lifetime on reads`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        readMetadata("remote-cached")
        remoteFiles["$CENTRAL/$METADATA"] = RELEASES_METADATA

        now = now.plusSeconds(59)
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0")
        now = now.plusSeconds(2)
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("3.0.0", "4.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @Test
    fun `should retry failed merges immediately`() {
        assertError(mavenFacade.findData(LookupRequest(null, "remote-cached", METADATA.toLocation())))
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @Test
    fun `should not cache merges that overlap with an invalidation`() {
        // given: metadata that is saved locally while a merge is still fetching remote sources
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        storeVersions("remote-cached", "3.0.0")
        val saved = AtomicBoolean()
        afterRemoteGet = {
            if (saved.compareAndSet(false, true)) {
                assertOk(mavenFacade.saveMetadata(SaveMetadataRequest(
                    mavenFacade.getRepository("remote-cached")!!,
                    GAV.toLocation(),
                    Metadata(groupId = "foo", artifactId = "bar", versioning = Versioning(_versions = listOf("3.0.0"))),
                )))
            }
        }

        // when: the overlapping merge completes with the state it has read before
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0")

        // then: its outdated result is not cached, so the next request sees the saved version
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0", "3.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @ParameterizedTest
    @ValueSource(strings = ["foo/other/maven-metadata.xml", "foo/other/1.0/other-1.0.jar", "foo/other/1.0/other-1.0.pom"])
    fun `should cache running merges when an unrelated artifact is deployed`(deployedFile: String) {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        val deployed = AtomicBoolean()
        afterRemoteGet = {
            if (deployed.compareAndSet(false, true)) {
                assertOk(mavenFacade.deployFile(DeployRequest(
                    repository = mavenFacade.getRepository("remote-cached")!!,
                    gav = deployedFile.toLocation(),
                    by = "test",
                    content = RELEASES_METADATA.byteInputStream(),
                    generateChecksums = false,
                )))
            }
        }

        // A write to another artifact during the first merge must not force another upstream lookup.
        repeat(2) {
            assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0")
        }
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
    }

    @Test
    fun `should refresh cached checksums after metadata is redeployed`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        storeVersions("remote-cached", "3.0.0", "4.0.0")
        val request = LookupRequest(UNAUTHORIZED, "remote-cached", METADATA.toLocation())
        fun read(file: String): ByteArray =
            assertOk(mavenFacade.findData(request.copy(gav = file.toLocation()))).use { it.readBytes() }

        val original = read(METADATA)
        Checksum.entries.forEach { checksum ->
            repeat(2) {
                assertThat(read("$METADATA.${checksum.extension}").decodeToString())
                    .isEqualTo(checksum.generate(original.inputStream()))
            }
        }

        assertOk(mavenFacade.deployFile(DeployRequest(
            repository = mavenFacade.getRepository("remote-cached")!!,
            gav = METADATA.toLocation(),
            by = "test",
            content = RELEASES_METADATA.byteInputStream(),
            generateChecksums = false,
        )))

        val updated = read(METADATA)
        assertThat(updated).isNotEqualTo(original)
        Checksum.entries.forEach { checksum ->
            assertThat(read("$METADATA.${checksum.extension}").decodeToString())
                .isEqualTo(checksum.generate(updated.inputStream()))
        }
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @Test
    fun `should not republish stale metadata when invalidation races with cache publication`() {
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        val publicationStarted = CountDownLatch(1)
        val releasePublication = CountDownLatch(1)
        val blockOnce = AtomicBoolean(true)
        afterRemoteGet = {
            // Caffeine reads its ticker inside put, after the service has checked the generation.
            beforeClockRead = {
                if (blockOnce.compareAndSet(true, false)) {
                    publicationStarted.countDown()
                    check(releasePublication.await(10, TimeUnit.SECONDS))
                }
            }
        }
        val executor = Executors.newFixedThreadPool(2)
        val journalist = InMemoryLogger()
        val service = MergedMetadataService(
            journalist,
            MirrorService(journalist, FailureComponents(journalist).failureFacade(), clock, executor),
            clock,
        )
        val repository = mavenFacade.getRepository("remote-cached")!!
        fun read(): Metadata = assertOk(service.findInputStream(repository, METADATA.toLocation(), null))
            .use { metadataXmlMapper.readValue(it) }

        try {
            val original = executor.submit<Metadata> { read() }
            assertThat(publicationStarted.await(10, TimeUnit.SECONDS)).isTrue()
            remoteFiles["$CENTRAL/$METADATA"] = RELEASES_METADATA
            val invalidating = CompletableFuture<Thread>()
            val invalidated = executor.submit {
                invalidating.complete(Thread.currentThread())
                service.invalidate(METADATA.toLocation())
            }
            val thread = invalidating.get(10, TimeUnit.SECONDS)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            // Either invalidation completes first or it waits for the pending publication to finish.
            while (!invalidated.isDone && thread.state != Thread.State.BLOCKED) {
                check(System.nanoTime() < deadline) { "Invalidation did not complete or wait for publication" }
                Thread.onSpinWait()
            }
            releasePublication.countDown()
            original.get(10, TimeUnit.SECONDS)
            invalidated.get(10, TimeUnit.SECONDS)

            assertThat(read().versioning?.versions).containsExactly("3.0.0", "4.0.0")
            assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
        } finally {
            releasePublication.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `should serve cached metadata while another merge is being published`() {
        // given: cached metadata of one artifact and the merge of another one that blocks while it holds the cache lock
        val other = "foo/baz/$METADATA_FILE"
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        remoteFiles["$CENTRAL/$other"] = CENTRAL_METADATA.replace("<artifactId>bar", "<artifactId>baz")
        val publicationStarted = CountDownLatch(1)
        val releasePublication = CountDownLatch(1)
        val publishing = CompletableFuture<Thread>()
        afterRemoteGet = { uri ->
            if (uri.endsWith(other)) {
                beforeClockRead = {
                    // Caffeine reads its ticker inside put, which runs under the cache lock when the merge is published
                    if (publishing.getNow(null) == Thread.currentThread() && Thread.currentThread().stackTrace.any { it.methodName == "publish" }) {
                        beforeClockRead = {}
                        publicationStarted.countDown()
                        check(releasePublication.await(10, TimeUnit.SECONDS))
                    }
                }
            }
        }
        val executor = Executors.newFixedThreadPool(4)
        val journalist = InMemoryLogger()
        val service = MergedMetadataService(
            journalist,
            MirrorService(journalist, FailureComponents(journalist).failureFacade(), clock, executor),
            clock,
        )
        val repository = mavenFacade.getRepository("remote-cached")!!
        fun read(file: String): Metadata = assertOk(service.findInputStream(repository, file.toLocation(), null))
            .use { metadataXmlMapper.readValue(it) }
        read(METADATA)

        try {
            val blocked = executor.submit<Metadata> {
                publishing.complete(Thread.currentThread())
                read(other)
            }
            assertThat(publicationStarted.await(10, TimeUnit.SECONDS)).isTrue()
            val invalidating = CompletableFuture<Thread>()
            val invalidated = executor.submit {
                invalidating.complete(Thread.currentThread())
                service.invalidate("unrelated/$METADATA_FILE".toLocation())
            }
            awaitBlocked(invalidating.get(10, TimeUnit.SECONDS))

            // when: the cached metadata is requested while the lock is held
            val cached = executor.submit<Metadata> { read(METADATA) }

            // then: the cache hit does not wait for the lock
            assertThat(cached.get(5, TimeUnit.SECONDS).versioning?.versions).containsExactly("1.0.0")
            assertThat(invalidated.isDone).isFalse()

            releasePublication.countDown()
            blocked.get(10, TimeUnit.SECONDS)
            invalidated.get(10, TimeUnit.SECONDS)
            assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
        } finally {
            releasePublication.countDown()
            executor.shutdownNow()
        }
    }

    private fun awaitBlocked(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (thread.state != Thread.State.BLOCKED) {
            check(System.nanoTime() < deadline) { "${thread.name} did not wait for the cache lock" }
            Thread.onSpinWait()
        }
    }

    @Test
    fun `should share one merge between concurrent requests`() {
        // given: a remote fetch that blocks until all concurrent requests are waiting
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        val fetchStarted = CountDownLatch(1)
        val releaseFetch = CountDownLatch(1)
        afterRemoteGet = {
            fetchStarted.countDown()
            releaseFetch.await(10, TimeUnit.SECONDS)
        }
        val executor = Executors.newFixedThreadPool(4)

        try {
            // when: one request starts the merge and three more ask for the same file in the meantime
            val leader = executor.submit<Metadata> { readMetadata("remote-uncached") }
            assertThat(fetchStarted.await(10, TimeUnit.SECONDS)).isTrue()
            val followers = (1..3).map {
                val waiting = CompletableFuture<Thread>()
                waiting to executor.submit<Metadata> {
                    waiting.complete(Thread.currentThread())
                    readMetadata("remote-uncached")
                }
            }
            followers.forEach { (waiting, _) -> awaitWaiting(waiting.get(10, TimeUnit.SECONDS)) }
            releaseFetch.countDown()

            // then: all requests receive the result of a single merge, even without a cache
            val results = listOf(leader.get(10, TimeUnit.SECONDS)) + followers.map { (_, result) -> result.get(10, TimeUnit.SECONDS) }
            assertThat(results).allSatisfy { assertThat(it.versioning?.versions).containsExactly("1.0.0") }
            assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
        } finally {
            releaseFetch.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `should fail waiting requests with the exception of the shared merge`() {
        // given: a merge that fails with an unexpected error after another request started waiting for it
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        val fetchStarted = CountDownLatch(1)
        val releaseFetch = CountDownLatch(1)
        afterRemoteGet = {
            fetchStarted.countDown()
            releaseFetch.await(10, TimeUnit.SECONDS)
            throw SimulatedFailure()
        }
        val executor = Executors.newFixedThreadPool(2)

        try {
            // when: the second request waits for the running merge
            val leader = executor.submit<Metadata> { readMetadata("remote-uncached") }
            assertThat(fetchStarted.await(10, TimeUnit.SECONDS)).isTrue()
            val waiting = CompletableFuture<Thread>()
            val follower = executor.submit<Metadata> {
                waiting.complete(Thread.currentThread())
                readMetadata("remote-uncached")
            }
            awaitWaiting(waiting.get(10, TimeUnit.SECONDS))
            releaseFetch.countDown()

            // then: both requests fail with the original error instead of a wrapped one
            for (request in listOf(leader, follower)) {
                assertThatThrownBy { request.get(10, TimeUnit.SECONDS) }
                    .isInstanceOf(ExecutionException::class.java)
                    .cause()
                    .isInstanceOf(SimulatedFailure::class.java)
            }
            assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)
        } finally {
            releaseFetch.countDown()
            executor.shutdownNow()
        }
    }

    /** An [Error] is not converted into an error response by the mirror service, so it reaches the merge. */
    private class SimulatedFailure : Error("Simulated failure")

    private fun awaitWaiting(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (thread.state != Thread.State.WAITING && thread.state != Thread.State.TIMED_WAITING) {
            check(System.nanoTime() < deadline) { "${thread.name} did not start waiting for the running merge" }
            Thread.onSpinWait()
        }
    }

    @Test
    fun `should merge metadata of all mirrored repositories`() {
        // given: an artifact with releases, snapshots and a version available in a remote repository
        publishReleasesAndSnapshots()
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        // when: the metadata file is requested from the aggregating repository
        val metadata = readMetadata("foo-all")

        // then: all versions are served in a single metadata file
        assertThat(metadata.groupId).isEqualTo("foo")
        assertThat(metadata.artifactId).isEqualTo("bar")
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0", "3.0.0-SNAPSHOT", "3.0.0", "4.0.0-SNAPSHOT", "4.0.0", "4.1.0-SNAPSHOT")
        assertThat(metadata.versioning?.latest).isEqualTo("4.1.0-SNAPSHOT")
        assertThat(metadata.versioning?.release).isEqualTo("4.0.0")
        assertThat(metadata.versioning?.lastUpdated).isEqualTo("20260219082051")
    }

    @Test
    fun `should expose snapshots of every mirrored repository`() {
        // given: builds of the same snapshot published to a local and to an external repository
        val snapshotGav = "$GAV/5.0.0-SNAPSHOT"
        addFileToRepository(FileSpec("foo-snapshots", "$snapshotGav/$METADATA_FILE", """
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <version>5.0.0-SNAPSHOT</version>
              <versioning>
                <snapshot><timestamp>20260301.100000</timestamp><buildNumber>1</buildNumber></snapshot>
                <lastUpdated>20260301100000</lastUpdated>
                <snapshotVersions>
                  <snapshotVersion><extension>jar</extension><value>5.0.0-20260301.100000-1</value><updated>20260301100000</updated></snapshotVersion>
                  <snapshotVersion><classifier>sources</classifier><extension>jar</extension><value>5.0.0-20260301.100000-1</value><updated>20260301100000</updated></snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
        """.trimIndent()))
        addFileToRepository(FileSpec("foo-snapshots", "$snapshotGav/bar-5.0.0-20260301.100000-1-sources.jar", "local sources"))
        remoteFiles["$CENTRAL/$snapshotGav/$METADATA_FILE"] = """
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <version>5.0.0-SNAPSHOT</version>
              <versioning>
                <snapshot><timestamp>20260302.100000</timestamp><buildNumber>7</buildNumber></snapshot>
                <lastUpdated>20260302100000</lastUpdated>
                <snapshotVersions>
                  <snapshotVersion><extension>jar</extension><value>5.0.0-20260302.100000-7</value><updated>20260302100000</updated></snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
        """.trimIndent()
        remoteFiles["$CENTRAL/$snapshotGav/bar-5.0.0-20260302.100000-7.jar"] = "remote jar"
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA.replace("<version>1.0.0</version>", "<version>1.0.0</version><version>5.0.0-SNAPSHOT</version>")
        publishReleasesAndSnapshots()

        // when: artifact and snapshot metadata are requested from the aggregating repository
        val artifactMetadata = readMetadata("foo-all")
        val snapshotMetadata = readMetadata("foo-all", "$snapshotGav/$METADATA_FILE")

        // then: the external snapshot is listed and each classifier points to its newest build
        assertThat(artifactMetadata.versioning?.versions).contains("5.0.0-SNAPSHOT", "4.1.0-SNAPSHOT")
        assertThat(snapshotMetadata.versioning?.snapshot?.buildNumber).isEqualTo(7)
        assertThat(snapshotMetadata.versioning?.snapshotVersions?.associate { it.classifier.orEmpty() to it.value })
            .isEqualTo(mapOf("" to "5.0.0-20260302.100000-7", "sources" to "5.0.0-20260301.100000-1"))

        // then: every referenced build can be resolved through the aggregating repository
        val versions = assertOk(mavenFacade.findVersions(VersionLookupRequest(null, mavenFacade.getRepository("foo-all")!!, snapshotGav.toLocation())))
        assertThat(versions.isSnapshot).isTrue()
        assertThat(versions.versions).containsExactly("5.0.0-20260301.100000-1", "5.0.0-20260302.100000-7")
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "foo-all", "$snapshotGav/bar-5.0.0-20260302.100000-7.jar".toLocation())))
        assertOk(mavenFacade.findFile(LookupRequest(UNAUTHORIZED, "foo-all", "$snapshotGav/bar-5.0.0-20260301.100000-1-sources.jar".toLocation())))
        val remoteJar = assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", "$snapshotGav/bar-5.0.0-20260302.100000-7.jar".toLocation())))
        assertThat(remoteJar.use { it.readBytes().decodeToString() }).isEqualTo("remote jar")
        val localSources = assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", "$snapshotGav/bar-5.0.0-20260301.100000-1-sources.jar".toLocation())))
        assertThat(localSources.use { it.readBytes().decodeToString() }).isEqualTo("local sources")
    }

    @Test
    fun `should serve the first match if merging is disabled`() {
        // given: an artifact published to both mirrored repositories
        publishReleasesAndSnapshots()

        // when: the metadata file is requested from a repository without merging
        val metadata = readMetadata("foo-first-match")

        // then: only the metadata of the first mirror is served
        assertThat(metadata.versioning?.versions).containsExactly("3.0.0", "4.0.0")
    }

    @Test
    fun `should include metadata deployed directly to the aggregating repository`() {
        // given: metadata in mirrors and a version deployed directly to the aggregating repository
        publishReleasesAndSnapshots()
        addFileToRepository(FileSpec("foo-all", METADATA, RELEASES_METADATA.replace("4.0.0", "5.0.0")))
        storeVersions("foo-all", "5.0.0")

        // when: the metadata file is requested
        val metadata = readMetadata("foo-all")

        // then: the local version is merged with mirrored ones
        assertThat(metadata.versioning?.versions).contains("5.0.0", "4.1.0-SNAPSHOT", "4.0.0")
        assertThat(metadata.versioning?.release).isEqualTo("5.0.0")
    }

    @Test
    fun `should not store merged metadata`() {
        // given: metadata available in a mirror configured to store files
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        publishReleasesAndSnapshots()

        // when: the metadata file is requested
        readMetadata("foo-all")

        // then: neither the merged nor the mirrored file is persisted in the aggregating repository
        assertThat(mavenFacade.getRepository("foo-all")!!.storageProvider.exists(METADATA.toLocation())).isFalse
    }

    @Test
    fun `should describe and checksum the merged file`() {
        // given: an artifact published to both mirrored repositories
        publishReleasesAndSnapshots()
        val content = assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", METADATA.toLocation()))).use { it.readBytes() }

        // when: details and checksums of the merged file are requested
        val details = assertOk(mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "foo-all", METADATA.toLocation())))

        // then: they describe the merged content
        assertThat(details).isInstanceOf(DocumentInfo::class.java)
        assertThat((details as DocumentInfo).contentLength).isEqualTo(content.size.toLong())

        Checksum.entries.forEach { checksum ->
            val checksumFile = "$METADATA.${checksum.extension}".toLocation()
            val served = assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", checksumFile))).use { it.readBytes().decodeToString() }
            assertThat(served).isEqualTo(checksum.generate(content.inputStream()))
            assertOk(mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "foo-all", checksumFile)))
        }
    }

    @Test
    fun `should not merge other files`() {
        // given: a pom published to both mirrored repositories
        addFileToRepository(FileSpec("foo-releases", "$GAV/4.0.0/bar-4.0.0.pom", "releases"))
        addFileToRepository(FileSpec("foo-snapshots", "$GAV/4.0.0/bar-4.0.0.pom", "snapshots"))

        // when: the pom is requested from the aggregating repository
        val content = assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", "$GAV/4.0.0/bar-4.0.0.pom".toLocation())))

        // then: the first match is served
        assertThat(content.use { it.readBytes().decodeToString() }).isEqualTo("releases")
    }

    @Test
    fun `should serve available metadata if some mirrors fail`() {
        // given: one healthy and one failing mirror
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        // when: the metadata file is requested
        val metadata = readMetadata("remote-cached")

        // then: the metadata of the healthy mirror is served
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0")
    }

    @Test
    fun `should fail if no mirror is available`() {
        // when: the metadata file is requested from a repository with a failing mirror only
        val error = assertError(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "remote-broken", METADATA.toLocation())))

        // then: the upstream failure is reported
        assertThat(error.status).isEqualTo(BAD_GATEWAY.code)
    }

    @Test
    fun `should report missing metadata`() {
        // when: metadata that does not exist anywhere is requested
        val error = assertError(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", "foo/missing/$METADATA_FILE".toLocation())))

        // then: it is not found
        assertThat(error.status).isEqualTo(404)
    }

    @Test
    fun `should reuse merged metadata within max age until it is redeployed`() {
        // given: a repository that keeps merged metadata for 60 seconds
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        storeVersions("remote-cached", "3.0.0", "4.0.0")
        val request = LookupRequest(UNAUTHORIZED, "remote-cached", METADATA.toLocation())

        // when: the metadata file, its details and checksum are requested
        assertOk(mavenFacade.findDetails(request))
        assertOk(mavenFacade.findData(request))
        assertOk(mavenFacade.findData(request.copy(gav = "$METADATA.sha1".toLocation())))

        // then: mirrors are requested only once
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)

        // when: new metadata is deployed to the repository
        assertOk(mavenFacade.deployFile(DeployRequest(
            repository = mavenFacade.getRepository("remote-cached")!!,
            gav = METADATA.toLocation(),
            by = "test",
            content = RELEASES_METADATA.byteInputStream(),
            generateChecksums = false,
        )))

        // then: the merged file is recalculated
        assertThat(readMetadata("remote-cached").versioning?.versions).containsExactly("1.0.0", "3.0.0", "4.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @Test
    fun `should not keep versions of mirrors that a client uploaded with its own version`() {
        // given: a client that deploys a version like Maven does - by extending the metadata served by the repository
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA.replace("<version>1.0.0</version>", "<version>1.0.0</version><version>1.1.0</version>")
        val repository = mavenFacade.getRepository("foo-all")!!
        val served = readMetadata("foo-all")
        assertThat(served.versioning?.versions).containsExactly("1.0.0", "1.1.0")
        storeVersions("foo-all", "5.0.0")
        val uploaded = served.copy(versioning = served.versioning!!.copy(latest = "5.0.0", release = "5.0.0", _versions = served.versioning.versions!! + "5.0.0"))
        assertOk(mavenFacade.deployFile(DeployRequest(repository, METADATA.toLocation(), "test", metadataXmlMapper.writeValueAsBytes(uploaded).inputStream(), false)))
        assertThat(readMetadata("foo-all").versioning?.versions).containsExactly("1.0.0", "1.1.0", "5.0.0")

        // when: the mirror retracts a version
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        // then: the version is gone, although the uploaded file still lists it
        val metadata = readMetadata("foo-all")
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0", "5.0.0")
        assertThat(metadata.versioning?.release).isEqualTo("5.0.0")

        // when: the mirror does not provide the artifact anymore
        remoteFiles.remove("$CENTRAL/$METADATA")

        // then: only the deployed version is left
        assertThat(readMetadata("foo-all").versioning?.versions).containsExactly("5.0.0")
    }

    @Test
    fun `should ignore mirrored metadata that was stored before merging was enabled`() {
        // given: a copy of mirrored metadata in the storage of the repository and an artifact cached from that mirror
        addFileToRepository(FileSpec("foo-all", METADATA, CENTRAL_METADATA.replace("<version>1.0.0</version>", "<version>0.9.0</version><version>1.0.0</version><version>1.1.0</version>")))
        storeVersions("foo-all", "0.9.0")
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        // when: the metadata file is requested
        val versions = readMetadata("foo-all").versioning?.versions

        // then: outdated versions are only listed if the repository can still serve them
        assertThat(versions).containsExactly("0.9.0", "1.0.0")
    }

    @Test
    fun `should report missing metadata if the stored file only lists unavailable versions`() {
        // given: a stored metadata file whose versions are neither stored nor provided by a mirror
        addFileToRepository(FileSpec("foo-all", METADATA, CENTRAL_METADATA))

        // when: the metadata file is requested
        val error = assertError(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", METADATA.toLocation())))

        // then: it is not found
        assertThat(error.status).isEqualTo(404)
    }

    @Test
    fun `should only merge stored snapshot builds of the repository`() {
        // given: snapshot metadata that lists a build of a mirror next to a build stored in the repository
        val snapshotGav = "$GAV/5.0.0-SNAPSHOT"
        addFileToRepository(FileSpec("foo-all", "$snapshotGav/$METADATA_FILE", """
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <version>5.0.0-SNAPSHOT</version>
              <versioning>
                <snapshot><timestamp>20260301.100000</timestamp><buildNumber>1</buildNumber></snapshot>
                <lastUpdated>20260301100000</lastUpdated>
                <snapshotVersions>
                  <snapshotVersion><extension>jar</extension><value>5.0.0-20260301.100000-1</value><updated>20260301100000</updated></snapshotVersion>
                  <snapshotVersion><classifier>sources</classifier><extension>jar</extension><value>5.0.0-20260201.100000-9</value><updated>20260201100000</updated></snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
        """.trimIndent()))
        addFileToRepository(FileSpec("foo-all", "$snapshotGav/bar-5.0.0-20260301.100000-1.jar", "local jar"))

        // when: the snapshot metadata is requested
        val versioning = readMetadata("foo-all", "$snapshotGav/$METADATA_FILE").versioning

        // then: only the stored build is served
        assertThat(versioning?.snapshot?.buildNumber).isEqualTo(1)
        assertThat(versioning?.snapshotVersions?.map { it.classifier.orEmpty() to it.value }).containsExactly("" to "5.0.0-20260301.100000-1")
    }

    @Test
    fun `should not add files to repositories that store mirrored metadata`() {
        // given: a repository without merging that stores mirrored files
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA

        // when: the metadata file is requested
        readMetadata("remote-stored")

        // then: only the mirrored file is stored
        val files = assertOk(mavenFacade.getRepository("remote-stored")!!.storageProvider.getFiles(GAV.toLocation()))
        assertThat(files.map { it.getSimpleName() }).containsExactly(METADATA_FILE)
    }

    @Test
    fun `should query remote mirrors concurrently next to mirrored local repositories`() {
        // given: a repository that mirrors a local repository and two remote repositories that wait for each other
        publishReleasesAndSnapshots()
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        remoteFiles["$OTHER/$METADATA"] = CENTRAL_METADATA.replace("1.0.0", "2.0.0")
        val requested = CountDownLatch(2)
        val concurrent = AtomicBoolean(true)
        afterRemoteGet = {
            requested.countDown()
            if (!requested.await(10, TimeUnit.SECONDS)) concurrent.set(false)
        }

        // when: the metadata file is requested
        val metadata = readMetadata("mixed")

        // then: both remote repositories have been requested at the same time and all sources are merged
        assertThat(concurrent).isTrue()
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0", "2.0.0", "3.0.0", "4.0.0")
    }

    @Test
    fun `should merge on the calling thread if the io pool is saturated`() {
        // given: an io pool whose only thread is busy
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        remoteFiles["$OTHER/$METADATA"] = CENTRAL_METADATA.replace("1.0.0", "2.0.0")
        val executor = Executors.newFixedThreadPool(1)
        val releasePool = CountDownLatch(1)
        executor.submit { releasePool.await() }
        val journalist = InMemoryLogger()
        val service = MergedMetadataService(
            journalist,
            MirrorService(journalist, FailureComponents(journalist).failureFacade(), clock, executor),
            clock,
        )

        try {
            // when: the metadata file is requested from a repository with several remote mirrors
            val merged = CompletableFuture.supplyAsync {
                assertOk(service.findInputStream(mavenFacade.getRepository("mixed")!!, METADATA.toLocation(), null))
                    .use { metadataXmlMapper.readValue<Metadata>(it) }
            }

            // then: the request does not wait for the pool
            assertThat(merged.get(10, TimeUnit.SECONDS).versioning?.versions).containsExactly("1.0.0", "2.0.0")
        } finally {
            releasePool.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `should remember missing metadata within max age`() {
        // given: metadata that does not exist in any repository
        val request = LookupRequest(UNAUTHORIZED, "central-cached", METADATA.toLocation())
        repeat(3) { assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404) }
        assertThat(assertError(mavenFacade.findData(request.copy(gav = "$METADATA.sha1".toLocation()))).status).isEqualTo(404)

        // then: mirrors are requested only once
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(1)

        // when: the file appears upstream and max age has passed
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404)
        now = now.plusSeconds(61)

        // then: it is served
        assertThat(readMetadata("central-cached").versioning?.versions).containsExactly("1.0.0")
        assertThat(remoteRequests["$CENTRAL/$METADATA"]?.get()).isEqualTo(2)
    }

    @Test
    fun `should forget missing metadata when it is deployed`() {
        // given: missing metadata that is remembered
        val request = LookupRequest(UNAUTHORIZED, "central-cached", METADATA.toLocation())
        assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404)

        // when: the artifact is deployed
        storeVersions("central-cached", "3.0.0", "4.0.0")
        assertOk(mavenFacade.deployFile(DeployRequest(mavenFacade.getRepository("central-cached")!!, METADATA.toLocation(), "test", RELEASES_METADATA.byteInputStream(), false)))

        // then: its metadata is served immediately
        assertThat(readMetadata("central-cached").versioning?.versions).containsExactly("3.0.0", "4.0.0")
    }

    @Test
    fun `should skip previously available mirrors when they fail`() {
        // given: two remote mirrors that have been merged before
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        remoteFiles["$OTHER/$METADATA"] = CENTRAL_METADATA.replace("1.0.0", "2.0.0")
        assertThat(readMetadata("mixed").versioning?.versions).containsExactly("1.0.0", "2.0.0")

        // when: one of them is unavailable and the other one publishes a new version
        remoteFailures[OTHER] = SERVICE_UNAVAILABLE
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA.replace("1.0.0", "1.5.0")

        // then: only the current versions of the available mirror are listed
        assertThat(readMetadata("mixed").versioning?.versions).containsExactly("1.5.0")
    }

    @Test
    fun `should invalidate parent metadata when backing artifacts appear or are removed`() {
        val repository = mavenFacade.getRepository("central-cached")!!
        val request = LookupRequest(null, repository.name, METADATA.toLocation())
        addFileToRepository(FileSpec(repository.name, METADATA, CENTRAL_METADATA))
        assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404)

        assertOk(mavenFacade.deployFile(DeployRequest(repository, "$GAV/1.0.0/bar-1.0.0.pom".toLocation(), "test", "pom".byteInputStream(), false)))
        assertThat(readMetadata(repository.name).versioning?.versions).containsExactly("1.0.0")

        val token = createAccessToken("delete", "secret", repository.name, "foo", WRITE)
        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, "$GAV/1.0.0".toLocation(), "test")))
        assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404)
    }

    @Test
    fun `should report missing metadata if generated metadata only lists unavailable versions`() {
        // given: metadata written by Reposilite itself, which contains empty elements, without the listed version
        val repository = mavenFacade.getRepository("central-cached")!!
        assertOk(mavenFacade.saveMetadata(SaveMetadataRequest(repository, GAV.toLocation(), Metadata(groupId = "foo", artifactId = "bar", versioning = Versioning(_versions = listOf("3.0.0"))))))
        val request = LookupRequest(null, repository.name, METADATA.toLocation())

        // then: it is not found until the version is stored
        assertThat(assertError(mavenFacade.findData(request)).status).isEqualTo(404)
        storeVersions(repository.name, "3.0.0")
        mavenFacade.deployFile(DeployRequest(repository, "$GAV/3.0.0/bar-3.0.0.jar".toLocation(), "test", "jar".byteInputStream(), false))
        assertThat(readMetadata(repository.name).versioning?.versions).containsExactly("3.0.0")
    }

    @Test
    fun `should not list versions and plugins whose directory is empty`() {
        // given: a version and a plugin that are stored in the repository
        val repository = mavenFacade.getRepository("central-cached")!!
        val token = createAccessToken("delete", "secret", repository.name, "foo", WRITE)
        fun deploy(file: String, content: String) = assertOk(mavenFacade.deployFile(DeployRequest(repository, file.toLocation(), "test", content.byteInputStream(), false)))
        deploy("$GAV/1.0.0/bar-1.0.0.pom", "pom")
        deploy("$GAV/1.0.0/bar-1.0.0.jar", "jar")
        deploy("$GAV/2.0.0/bar-2.0.0.pom", "pom")
        deploy(METADATA, CENTRAL_METADATA.replace("<version>1.0.0</version>", "<version>1.0.0</version><version>2.0.0</version>").replace("<release>1.0.0", "<release>2.0.0").replace("<latest>1.0.0", "<latest>2.0.0"))
        deploy("foo/a-maven-plugin/1.0/a-maven-plugin-1.0.pom", "pom")
        deploy("foo/$METADATA_FILE", "<metadata><plugins><plugin><name>A</name><prefix>a</prefix><artifactId>a-maven-plugin</artifactId></plugin></plugins></metadata>")
        assertThat(readMetadata(repository.name).versioning?.versions).containsExactly("1.0.0", "2.0.0")
        assertThat(readMetadata(repository.name, "foo/$METADATA_FILE").plugins?.map { it.artifactId }).containsExactly("a-maven-plugin")

        // when: the files of a version are deleted one by one, which leaves its directory behind
        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, "$GAV/2.0.0/bar-2.0.0.pom".toLocation(), "test")))
        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, "$GAV/1.0.0/bar-1.0.0.pom".toLocation(), "test")))

        // then: only the version that still has a file is listed
        assertThat(repository.storageProvider.exists("$GAV/2.0.0".toLocation())).isTrue()
        val metadata = readMetadata(repository.name)
        assertThat(metadata.versioning?.versions).containsExactly("1.0.0")
        assertThat(metadata.versioning?.release).isEqualTo("1.0.0")
        assertThat(metadata.versioning?.latest).isEqualTo("1.0.0")

        // when: the last files of the remaining version and of the plugin are deleted
        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, "$GAV/1.0.0/bar-1.0.0.jar".toLocation(), "test")))
        assertOk(mavenFacade.deleteFile(DeleteRequest(token, repository, "foo/a-maven-plugin/1.0".toLocation(), "test")))

        // then: neither the artifact nor the plugin are found anymore
        assertThat(assertError(mavenFacade.findData(LookupRequest(null, repository.name, METADATA.toLocation()))).status).isEqualTo(404)
        assertThat(assertError(mavenFacade.findData(LookupRequest(null, repository.name, "foo/$METADATA_FILE".toLocation()))).status).isEqualTo(404)
    }

    @Test
    fun `should not serve signatures of mirrored metadata`() {
        // given: a mirror that provides a signature of its metadata file
        remoteFiles["$CENTRAL/$METADATA"] = CENTRAL_METADATA
        remoteFiles["$CENTRAL/$METADATA.asc"] = "signature of the mirrored file"

        // then: it is only served by repositories that do not merge metadata
        assertOk(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "remote-stored", "$METADATA.asc".toLocation()))).close()
        assertThat(assertError(mavenFacade.findData(LookupRequest(UNAUTHORIZED, "foo-all", "$METADATA.asc".toLocation()))).status).isEqualTo(404)
        assertThat(assertError(mavenFacade.findDetails(LookupRequest(UNAUTHORIZED, "foo-all", "$METADATA.asc.sha1".toLocation()))).status).isEqualTo(404)
        assertThat(remoteRequests["$CENTRAL/$METADATA.asc"]?.get()).isEqualTo(1)
    }

}
