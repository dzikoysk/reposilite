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

import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.api.ResolvedFileEvent
import com.reposilite.maven.application.RepositorySettings
import com.reposilite.maven.specification.MavenSpecification
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
