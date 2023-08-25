@file:Suppress("FunctionName")

package com.reposilite.maven

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.RecommendedRemoteSpecificationJunitExtension
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.specification.MavenIntegrationSpecification
import com.reposilite.storage.api.toLocation
import kong.unirest.Unirest
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
            val notFoundResponse = Unirest.get("$base/proxied/not/found.jar").asString()

            // then: service responds with 404 status page
            assertThat(notFoundResponse.isSuccess).isFalse

            // when: file that exists in remote repository is requested
            val response = Unirest.get("$base/proxied/$gav").asString()

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
            val response = Unirest.get("$base/proxied/$gav").asString()
            // then: service responds with 404 status page as .file extension is not allowed
            assertThat(response.isSuccess).isFalse
        }
    }

    @Test
    fun `should prioritize upstream metadata file over local copy`() = runBlocking {
        // given: a remote server and artifact
        useProxiedHost("releases", "com/reposilite/maven-metadata.xml", "upstream") { gav, _ ->
            // and: local repository with cached metadata file
            useDocument("proxied", "com/reposilite", "maven-metadata.xml", "local", true)

            // when: metadata file is requested
            val response = Unirest.get("$base/proxied/$gav").asString()

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
            assertThat(localFile.get().second.readAllBytes().decodeToString()).isEqualTo("upstream")
        }
    }

}