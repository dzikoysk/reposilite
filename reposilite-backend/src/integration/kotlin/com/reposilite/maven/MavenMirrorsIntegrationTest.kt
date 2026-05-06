@file:Suppress("FunctionName")

package com.reposilite.maven

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.storage.api.toLocation
import io.javalin.Javalin
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kong.unirest.core.Unirest.get
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalMavenMirrorsIntegrationTest : MavenMirrorsIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteMavenMirrorsIntegrationTest : MavenMirrorsIntegrationTest()

internal abstract class MavenMirrorsIntegrationTest : MavenIntegrationSpecification() {

    @Test
    fun `should proxy remote file`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/remote.jar", "content") { gav, content ->
            // when: non-existing file is requested
            val notFoundResponse = get("$base/proxied/not/found.jar").asString()

            // then: service responds with 404 status page
            assertThat(notFoundResponse.isSuccess).isFalse

            // when: file that exists in remote repository is requested
            val response = get("$base/proxied/$gav").asString()

            // then: service responds with its content
            assertThat(response.body).isEqualTo(content)
            assertThat(response.isSuccess).isTrue
        }
    }

    @Test
    fun `should not proxy file with forbidden extension`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/remote.file", "content") { gav, _ ->
            // when: file that exists in remote repository is requested
            val response = get("$base/proxied/$gav").asString()
            // then: service responds with 404 status page as .file extension is not allowed
            assertThat(response.isSuccess).isFalse
        }
    }

    @Test
    fun `should not respond with 200 and empty body when upstream HEAD succeeds but GET fails`() {
        // given: an upstream where HEAD reports the artifact exists but GET fails (network blip / 5xx mid-fetch)
        val gav = "com/reposilite/broken.jar"
        val started = CountDownLatch(1)
        val upstream = Javalin.start { config ->
            config.jetty.port = reposilite.parameters.port + 1
            config.events.serverStarted { started.countDown() }
            config.routes.head("/releases/$gav") { ctx -> ctx.contentType("application/java-archive").header("Content-Length", "100").status(200) }
            config.routes.get("/releases/$gav") { ctx -> ctx.status(500).result("upstream temporarily unavailable") }
        }
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue

        try {
            // when: client requests the artifact through the proxy
            val response = get("$base/proxied/$gav").asString()

            // then: response must not be 200-with-empty-body — Aether interprets that as "no checksums available" for .sha1 lookups
            assertThat(response.status == 200 && response.body.isNullOrEmpty()).isFalse
        } finally {
            upstream.stop()
        }
    }

    @Test
    fun `should not duplicate concurrent mirror downloads for the same artifact`() {
        // given: a remote server that counts how many times the artifact is fetched
        val gav = "com/reposilite/concurrent.jar"
        val content = "concurrent-content"
        val upstreamHits = AtomicInteger(0)
        val started = CountDownLatch(1)
        val upstream = Javalin.start { config ->
            config.jetty.port = reposilite.parameters.port + 1
            config.events.serverStarted { started.countDown() }
            config.routes.head("/releases/$gav") { ctx ->
                ctx.contentType("application/java-archive").header("Content-Length", content.length.toString()).status(200)
            }
            config.routes.get("/releases/$gav") { ctx ->
                upstreamHits.incrementAndGet()
                Thread.sleep(300)
                ctx.result(content)
            }
        }
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue

        try {
            // when: five clients request the same artifact in parallel through a storing mirror
            val executor = Executors.newFixedThreadPool(5)
            try {
                val responses = (1..5)
                    .map { executor.submit(Callable { get("$base/proxied-stored/$gav").asString() }) }
                    .map { it.get(30, TimeUnit.SECONDS) }

                // then: every client gets the artifact and the upstream is contacted only once
                assertThat(responses).allSatisfy { response ->
                    assertThat(response.status).isEqualTo(200)
                    assertThat(response.body).isEqualTo(content)
                }
                assertThat(upstreamHits.get()).isEqualTo(1)
            } finally {
                executor.shutdownNow()
            }
        } finally {
            upstream.stop()
        }
    }

    @Test
    fun `should prioritize upstream metadata file over local copy`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/maven-metadata.xml", "upstream") { gav, _ ->
            // and: local repository with cached metadata file
            useDocument("proxied", "com/reposilite", "maven-metadata.xml", "local", true)

            // when: metadata file is requested
            val response = get("$base/proxied/$gav").asString()

            // then: service responds with upstream metadata file
            assertThat(response.body).isEqualTo("upstream")
            assertThat(response.isSuccess).isTrue

            // and: local metadata file is updated
            val localFile = mavenFacade.findFile(
                LookupRequest(
                    accessToken = null,
                    repository = "proxied",
                    gav = "com/reposilite/maven-metadata.xml".toLocation(),
                )
            )
            assertThat(localFile.isOk).isTrue
            assertThat(localFile.get().content.readAllBytes().decodeToString()).isEqualTo("upstream")
        }
    }

}