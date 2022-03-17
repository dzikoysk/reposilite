/*
 * Copyright (c) 2022 dzikoysk
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

@file:Suppress("FunctionName")

package com.reposilite.maven

import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.token.RoutePermission.READ
import io.javalin.http.HttpCode.UNAUTHORIZED
import kong.unirest.Unirest.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal abstract class MavenApiIntegrationTest : MavenIntegrationSpecification() {

    @ValueSource(strings = [
        "api/maven/versions",
        "api/maven/latest/version",
        "api/maven/latest/details",
        "api/maven/latest/file",
    ])
    @ParameterizedTest
    fun `should find latest version`(endpoint: String) {
        // given: a path to the existing artifact
        val latestVersion = "1.0.3"
        val (repository, metadata) = useMetadata("private", "com", "reposilite", versions = listOf("1.0.1", "1.0.2", latestVersion))
        val (_) = useDocument(repository, "${metadata.groupId}/${metadata.artifactId}/$latestVersion", "${metadata.artifactId}-$latestVersion-fat.panda", "content", true)
        val artifactPath = "private/com/reposilite"
        val apiPath = "$base/$endpoint/$artifactPath?extension=panda?classifier=fat"

        // when: user requests the latest version with invalid credentials
        val unauthorizedResponse = get(apiPath)
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: valid credentials
        val (name, secret) = useAuth("name", "secret", routes = mapOf("/$artifactPath" to READ))

        // when: user requests the latest version with invalid credentials
        val response = get(apiPath)
            .basicAuth(name, secret)
            .asString()

        // then: the request should succeed
        assertTrue(response.isSuccess)
    }

    @ValueSource(strings = [
        "/api/maven/details/private",
        "/api/maven/details/private/gav",
        "/api/maven/details/private/gav/artifact.jar",
    ])
    @ParameterizedTest
    fun `should respond with protected file details only for authenticated requests`(endpoint: String) {
        // given: a private repository with some artifact
        useDocument("private", "gav", "artifact.jar", store = true)

        // when: user requests private resource without valid credentials
        val unauthorizedResponse = get("$base$endpoint")
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: valid credentials
        val (name, secret) = useAuth("name", "secret", routes = mapOf(endpoint.replace("/api/maven/details", "") to READ))

        // when: user requests private resource with valid credentials
        val response = get("$base$endpoint")
            .basicAuth(name, secret)
            .asString()

        // then: service responds with file details
        assertTrue(response.isSuccess)
    }

    @Test
    fun `should resolve latest version of snapshot`() {
        val versionIdentifier = "1.0-20211230.200052-3"
        val (repository, gav, _, content) = useDocument("private", "group/artifact/version-SNAPSHOT", "artifact-$versionIdentifier.jar", "content", true)
        val (_) = useDocument(repository, gav, "maven-metadata.xml", """
           <metadata modelVersion="1.1.0">
             <version>1.0-SNAPSHOT</version>
             <versioning>
               <snapshotVersions>
                 <snapshotVersion>
                   <extension>jar</extension>
                   <value>$versionIdentifier</value>
                   <updated>20211230200052</updated>
                 </snapshotVersion>
               </snapshotVersions>
             </versioning>
           </metadata>
        """.trimIndent(), true)
        val apiUrl = "$base/api/maven/latest/file/$repository/$gav"

        // when: user requests the latest version with invalid credentials
        val unauthorizedResponse = get(apiUrl)
            .basicAuth("invalid", "invalid-secret")
            .asString()

        // then: service rejects request
        assertEquals(UNAUTHORIZED.status, unauthorizedResponse.status)

        // given: valid credentials
        val (name, secret) = useAuth("name", "secret", routes = mapOf("/private" to READ))

        // when: user requests the latest version with invalid credentials
        val response = get(apiUrl)
            .basicAuth(name, secret)
            .asString()

        // then: the request should succeed
        assertTrue(response.isSuccess)
        assertEquals(content, response.body)
    }

}