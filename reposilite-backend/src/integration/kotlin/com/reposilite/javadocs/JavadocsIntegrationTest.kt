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
