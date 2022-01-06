/*
 * Copyright (c) 2021 dzikoysk
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

package com.reposilite.token

internal interface AccessTokenRepository {

    fun saveAccessToken(accessToken: AccessToken): AccessToken

    fun deleteAccessToken(id: AccessTokenId)

    fun findAccessTokenById(id: AccessTokenId): AccessToken?

    fun findAccessTokenByName(name: String): AccessToken?

    fun addPermission(id: AccessTokenId, permission: AccessTokenPermission)

    fun deletePermission(id: AccessTokenId, permission: AccessTokenPermission)

    fun findAccessTokenPermissionsById(id: AccessTokenId): AccessTokenPermissions

    fun addRoute(id: AccessTokenId, route: Route)

    fun deleteRoute(id: AccessTokenId, route: Route)

    fun findAccessTokenRoutesById(id: AccessTokenId): Routes

    fun findAll(): Collection<AccessToken>

    fun countAccessTokens(): Long

}