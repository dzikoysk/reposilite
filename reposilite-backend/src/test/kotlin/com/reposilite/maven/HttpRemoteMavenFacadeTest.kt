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

package com.reposilite.maven

import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.application.MirroredRepositorySettings
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
import com.reposilite.shared.http.HttpRemoteClientProvider
import com.reposilite.shared.http.RemoteClientProvider
import com.reposilite.storage.api.toLocation
import io.javalin.Javalin
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.TOO_MANY_REQUESTS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import panda.std.ResultAssertions.assertError

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HttpRemoteMavenFacadeTest : MavenSpecification() {

    private companion object {
        const val JAR = "org/example/library/1.0/library-1.0.jar"
        const val POM = "org/example/library/1.0/library-1.0.pom"
    }

    private lateinit var upstream: Javalin
    private lateinit var upstreamUrl: String

    @BeforeAll
    fun startUpstream() {
        upstream = Javalin.create { config ->
            listOf(JAR, POM).forEach { gav ->
                config.routes.head("/rate-limited/$gav") { ctx ->
                    ctx.contentType("text/html").status(TOO_MANY_REQUESTS)
                }
                config.routes.head("/missing/$gav") { ctx ->
                    ctx.contentType("application/xml").status(NOT_FOUND)
                }
            }
            config.routes.head("/html/$POM") { ctx ->
                ctx.contentType("text/html").status(OK)
            }
        }.start(0)
        upstreamUrl = "http://127.0.0.1:${upstream.port()}"
    }

    @AfterAll
    fun stopUpstream() {
        upstream.stop()
    }

    override val remoteClientProviderOverride: RemoteClientProvider =
        HttpRemoteClientProvider

    override fun repositories(): List<RepositorySettings> = listOf(
        repository("RATE_LIMITED", "rate-limited"),
        repository("MISSING", "missing"),
        repository("HTML", "html"),
        repository("MULTI", "rate-limited", "missing"),
    )

    @ParameterizedTest
    @ValueSource(strings = [JAR, POM])
    fun `should expose upstream rate limit through Maven facade`(gav: String) {
        val result = findFile("RATE_LIMITED", gav)

        assertError(result)
        assertThat(result.error.status).isEqualTo(429)
    }

    @ParameterizedTest
    @ValueSource(strings = [JAR, POM])
    fun `should expose upstream not found through Maven facade`(gav: String) {
        val result = findFile("MISSING", gav)

        assertError(result)
        assertThat(result.error.status).isEqualTo(404)
    }

    @Test
    fun `should reject successful HTML response through Maven facade`() {
        val result = findFile("HTML", POM)

        assertError(result)
    }

    @Test
    fun `should retain first mirror 429 when second mirror returns 404`() {
        val result = findFile("MULTI", JAR)

        assertError(result)
        assertThat(result.error.status).isEqualTo(429)
    }

    private fun repository(name: String, vararg paths: String): RepositorySettings =
        RepositorySettings(
            id = name,
            proxied = paths.map { path -> MirroredRepositorySettings(reference = "$upstreamUrl/$path") },
        )

    private fun findFile(repository: String, gav: String) =
        mavenFacade.findFile(LookupRequest(UNAUTHORIZED, repository, gav.toLocation()))

}
