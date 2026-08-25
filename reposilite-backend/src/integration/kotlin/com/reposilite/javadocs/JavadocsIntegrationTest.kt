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

@file:Suppress("FunctionName")

package com.reposilite.javadocs

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.configuration.shared.SharedConfigurationFacade
import com.reposilite.javadocs.application.JavadocSettings
import com.reposilite.javadocs.specification.JavadocsIntegrationSpecification
import com.reposilite.storage.api.Location
import io.javalin.http.HttpStatus.NOT_FOUND
import kong.unirest.core.Unirest.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class LocalJavadocsIntegrationTest : JavadocsIntegrationTest()

@ExtendWith(RecommendedRemoteSpecificationJunitExtension::class)
internal class RemoteJavadocsIntegrationTest : JavadocsIntegrationTest()

internal abstract class JavadocsIntegrationTest : JavadocsIntegrationSpecification() {

    @Test
    fun `should serve javadocs`() {
        // given: some javadocs file & metadata file
        val (repository, metadata) = useMetadata(
            repository = "releases",
            groupId = "gav",
            artifactId = "reposilite",
            versions = listOf("3.0.0")
        )

        mavenFacade.getRepository(repository)!!
            .storageProvider
            .putFile(
                location = Location.of("${metadata.groupId}/${metadata.artifactId}/3.0.0/reposilite-3.0.0-javadoc.jar"),
                inputStream = JavadocsIntegrationTest::class.java.getResourceAsStream("/reposilite-javadoc.jar")!!
            )

        // when: client requests javadocs
        val response = get("$base/javadoc/releases/gav/reposilite/3.0.0")
            .asString()

        // then: response contains javadocs container
        assertThat(response.body).contains("""iframe id="javadoc"""")
    }

    @Test
    fun `should serve a non-javadoc suffix through the viewer`() {
        // given: an artifact published only with a groovydoc suffix (served by default)
        val (repository, metadata) = useMetadata(
            repository = "releases",
            groupId = "gav",
            artifactId = "reposilite",
            versions = listOf("3.0.0")
        )

        mavenFacade.getRepository(repository)!!
            .storageProvider
            .putFile(
                location = Location.of("${metadata.groupId}/${metadata.artifactId}/3.0.0/reposilite-3.0.0-groovydoc.jar"),
                inputStream = JavadocsIntegrationTest::class.java.getResourceAsStream("/reposilite-javadoc.jar")!!
            )

        // when: client requests javadocs through the directory url
        val response = get("$base/javadoc/releases/gav/reposilite/3.0.0")
            .asString()

        // then: response contains javadocs container
        assertThat(response.body).contains("""iframe id="javadoc"""")
    }

    @Test
    fun `should serve a non-jar extension when its suffix is configured`() {
        // given: a zip artifact and a configuration that serves the -docs.zip suffix
        val (repository, metadata) = useMetadata(
            repository = "releases",
            groupId = "gav",
            artifactId = "reposilite",
            versions = listOf("3.0.0")
        )

        mavenFacade.getRepository(repository)!!
            .storageProvider
            .putFile(
                location = Location.of("${metadata.groupId}/${metadata.artifactId}/3.0.0/reposilite-3.0.0-docs.zip"),
                inputStream = JavadocsIntegrationTest::class.java.getResourceAsStream("/reposilite-javadoc.jar")!!
            )

        useFacade<SharedConfigurationFacade>()
            .getDomainSettings<JavadocSettings>()
            .update(JavadocSettings(suffixes = listOf("-javadoc.jar", "-docs.zip")))

        // when: client requests javadocs through the directory url
        val response = get("$base/javadoc/releases/gav/reposilite/3.0.0")
            .asString()

        // then: response contains javadocs container
        assertThat(response.body).contains("""iframe id="javadoc"""")
    }

    @Test
    fun `should respond with not found when javadoc integration is disabled`() {
        // given: some javadocs file & metadata file with the javadoc integration disabled
        val (repository, metadata) = useMetadata(
            repository = "releases",
            groupId = "gav",
            artifactId = "reposilite",
            versions = listOf("3.0.0")
        )

        mavenFacade.getRepository(repository)!!
            .storageProvider
            .putFile(
                location = Location.of("${metadata.groupId}/${metadata.artifactId}/3.0.0/reposilite-3.0.0-javadoc.jar"),
                inputStream = JavadocsIntegrationTest::class.java.getResourceAsStream("/reposilite-javadoc.jar")!!
            )

        useFacade<SharedConfigurationFacade>()
            .getDomainSettings<JavadocSettings>()
            .update(JavadocSettings(enabled = false))

        // when: client requests javadocs
        val response = get("$base/javadoc/releases/gav/reposilite/3.0.0")
            .asString()

        // then: response is not found and does not leak the disabled state
        assertThat(response.status).isEqualTo(NOT_FOUND.code)
        assertThat(response.body).doesNotContain("disabled")
    }

}
