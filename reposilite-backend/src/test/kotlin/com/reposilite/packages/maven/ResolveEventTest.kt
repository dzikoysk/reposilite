package com.reposilite.packages.maven

import com.reposilite.packages.maven.api.LookupRequest
import com.reposilite.packages.maven.api.ResolvedFileEvent
import com.reposilite.packages.maven.application.RepositorySettings
import com.reposilite.packages.maven.specification.MavenSpecification
import com.reposilite.storage.api.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import panda.std.ResultAssertions.assertOk
import panda.std.asSuccess

internal class ResolveEventTest : MavenSpecification() {

    override fun repositories(): List<RepositorySettings> = listOf(
        RepositorySettings(id = "releases")
    )

    @Test
    fun `should properly respond with intercepted file `() {
        // given: repository with some files & resolved file event listener
        val gav = Location.of("/g/a/v/app.jar")
        addFileToRepository(FileSpec("releases", gav.toString(), "{placeholder}"))

        extensions.registerEvent { event: ResolvedFileEvent ->
            event.result = event.result.flatMap { (document, data) ->
                if (document.name == gav.getSimpleName()) {
                    data.readBytes()
                        .decodeToString()
                        .replace("{placeholder}", "content")
                        .byteInputStream()
                        .let { document.copy(contentLength = it.available().toLong()) to it }
                        .asSuccess()
                } else (document to data).asSuccess()
            }
        }

        // when: file is requested
        val result = mavenFacade.findFile(LookupRequest(null, "releases", gav))

        // then: listener properly intercepted result
        val (document, content) = assertOk(result)
        assertThat(content.readBytes().decodeToString()).isEqualTo("content")
        assertThat(document.contentLength).isEqualTo("content".encodeToByteArray().size.toLong())
    }

}
