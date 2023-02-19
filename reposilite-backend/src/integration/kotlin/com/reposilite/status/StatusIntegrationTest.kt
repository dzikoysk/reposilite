/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.status

import com.reposilite.VERSION
import com.reposilite.specification.ExperimentalLocalSpecificationJunitExtension
import com.reposilite.specification.ExperimentalRemoteSpecficiationJunitExtension
import com.reposilite.specification.LocalSpecificationJunitExtension
import com.reposilite.specification.RemoteSpecificationJunitExtension
import com.reposilite.status.api.InstanceStatusResponse
import com.reposilite.status.api.StatusSnapshot
import com.reposilite.status.specification.StatusIntegrationSpecification
import com.reposilite.token.AccessTokenPermission.MANAGER
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ExperimentalRemoteSpecficiationJunitExtension::class)
internal class ExperimentalRemoteStatusIntegrationTest : StatusIntegrationTest()

@ExtendWith(RemoteSpecificationJunitExtension::class)
internal class RemoteStatusIntegrationTest : StatusIntegrationTest()

@ExtendWith(ExperimentalLocalSpecificationJunitExtension::class)
internal class ExperimentalLocalStatusIntegrationTest : StatusIntegrationTest()

@ExtendWith(LocalSpecificationJunitExtension::class)
internal class LocalStatusIntegrationTest : StatusIntegrationTest()

internal abstract class StatusIntegrationTest : StatusIntegrationSpecification() {

    @Test
    fun `should respond with instance status`() {
        // when: status service is requested without valid credentials
        val unauthorizedResponse = Unirest.get("$base/api/status/instance").asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = Unirest.get("$base/api/status/instance")
            .basicAuth(name, secret)
            .asObject(InstanceStatusResponse::class.java)

        // then: service should respond with valid instance dto
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body.version).isEqualTo(VERSION)
    }

    @Test
    fun `should respond with status snapshots`() {
        // when: status service is requested without valid credentials
        val unauthorizedResponse = Unirest.get("$base/api/status/snapshots").asString()

        // then: service rejects request
        assertThat(unauthorizedResponse.status).isEqualTo(UNAUTHORIZED.code)

        // given: a valid credentials
        val (name, secret) = useAuth("name", "secret", listOf(MANAGER))

        // when: service is requested with valid credentials
        val response = Unirest.get("$base/api/status/snapshots")
            .basicAuth(name, secret)
            .asJacksonObject(Array<StatusSnapshot>::class)

        // then: service should respond with valid instance dto
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body.getOrNull(0)).isNotNull
    }

}