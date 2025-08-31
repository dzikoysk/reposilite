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

package com.reposilite.token.api

import com.reposilite.token.AccessTokenPermission
import com.reposilite.token.AccessTokenType
import com.reposilite.token.Route
import com.reposilite.token.RoutePermission
import com.reposilite.token.api.SecretType.RAW
import io.javalin.openapi.OpenApiDescription

enum class SecretType {
    RAW,
    ENCRYPTED
}

data class CreateAccessTokenRequest(
    val type: AccessTokenType,
    val name: String,
    val secretType: SecretType = RAW,
    val secret: String? = null,
    val permissions: Set<AccessTokenPermission> = emptySet(),
    val routes: Set<Route> = emptySet(),
) {
    data class Route(
        val path: String,
        val permissions: Set<RoutePermission> = emptySet(),
    )
}

data class CreateAccessTokenWithNoNameRequest(
    @get:OpenApiDescription("Type of the created token (persistent or temporary)")
    val type: AccessTokenType,
    @get:OpenApiDescription("Determines whether the provided secret is raw or already encrypted")
    val secretType: SecretType = RAW,
    @get:OpenApiDescription("If not provided, the secret will be generated automatically")
    val secret: String? = null,
    @get:OpenApiDescription("Permissions assigned to the created token: [MANAGER]")
    val permissions: Set<String> = emptySet(),
    @get:OpenApiDescription("Route permissions assigned to the created token")
    val routes: Set<Route> = emptySet(),
) {
    data class Route(
        @get:OpenApiDescription("The path to which the permissions apply")
        val path: String,
        @get:OpenApiDescription("Permissions assigned to the provided path: [READ, WRITE]")
        val permissions: Set<String> = emptySet(),
    )
}

data class CreateAccessTokenResponse(
    val accessToken: AccessTokenDto,
    val permissions: Set<AccessTokenPermission>,
    val routes: Set<Route>,
    val secret: String,
)
