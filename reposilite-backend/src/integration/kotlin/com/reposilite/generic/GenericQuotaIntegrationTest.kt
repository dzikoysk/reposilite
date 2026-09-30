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

package com.reposilite.generic

import com.reposilite.RecommendedLocalSpecificationJunitExtension
import com.reposilite.generic.application.GenericRepositorySettings
import com.reposilite.generic.specification.GenericIntegrationSpecification
import com.reposilite.shared.ErrorResponse
import com.reposilite.storage.filesystem.FileSystemStorageProviderSettings
import io.javalin.http.HttpStatus.INSUFFICIENT_STORAGE
import kong.unirest.core.Unirest.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RecommendedLocalSpecificationJunitExtension::class)
internal class GenericQuotaIntegrationTest : GenericIntegrationSpecification() {

    override fun repositories(): List<GenericRepositorySettings> =
        listOf(
            GenericRepositorySettings(
                id = "quota-files",
                storageProvider = FileSystemStorageProviderSettings(quota = "1MB"),
            )
        )

    @Test
    fun `should enforce storage quota`() {
        // given: content larger than the repository quota
        val (name, secret) = useDefaultManagementToken()
        val (content, _) = useFile("too-large.bin", 2)

        // when: the content is uploaded
        val response = put("$base/quota-files/too-large.bin")
            .basicAuth(name, secret)
            .body(content.inputStream())
            .asObject(ErrorResponse::class.java)

        // then: the upload is rejected because storage is insufficient
        assertThat(response.status).isEqualTo(INSUFFICIENT_STORAGE.code)
    }

}
