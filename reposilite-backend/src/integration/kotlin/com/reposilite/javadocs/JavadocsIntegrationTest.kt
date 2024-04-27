@file:Suppress("FunctionName")

package com.reposilite.javadocs

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.javadocs.specification.JavadocsIntegrationSpecification
import com.reposilite.storage.api.Location
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

}
