@file:Suppress("FunctionName")

package com.reposilite.javadocs

import com.reposilite.javadocs.specification.JavadocsIntegrationSpecification
import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.specification.RemoteSpecificationJunitExtension
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalJavadocsIntegrationTest : JavadocsIntegrationTest()

@ExtendWith(RemoteSpecificationJunitExtension::class)
internal class RemoteJavadocsIntegrationTest : JavadocsIntegrationTest()

internal abstract class JavadocsIntegrationTest : JavadocsIntegrationSpecification() {

    @Test
    fun `should serve javadocs`() {
        // given: some javadocs file
        val directory = reposiliteWorkingDirectory
            .resolve("repositories")
            .resolve("releases")
            .resolve("gav")
            .resolve("reposilite")
            .resolve("3.0.0")

        directory.mkdirs()

        Files.write(
            directory.resolve("reposilite-3.0.0-javadoc.jar").toPath(),
            JavadocsIntegrationTest::class.java.getResourceAsStream("/reposilite-javadoc.jar")!!.readAllBytes(),
            StandardOpenOption.CREATE_NEW
        )

        useMetadata(
            repository = "releases",
            groupId = "gav",
            artifactId = "reposilite",
            versions = listOf("3.0.0")
        )

        // when: client requests javadocs
        val response = Unirest.get("$base/javadoc/releases/gav/reposilite/3.0.0").asString()

        // then: response contains javadocs container
        assertThat(response.body).contains("""iframe id="javadoc"""")
    }

}
