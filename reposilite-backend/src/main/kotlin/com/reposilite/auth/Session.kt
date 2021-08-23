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
package com.reposilite.auth

import com.reposilite.token.api.AccessToken
import com.reposilite.token.api.AccessTokenPermission
import com.reposilite.token.api.RoutePermission
import java.nio.file.Path

data class Session internal constructor(
    val path: String,
    val method: SessionMethod,
    val address: String,
    val accessToken: AccessToken,
    val availableResources: List<Path>
) {

    companion object {
        val METHOD_PERMISSIONS = mapOf(
            SessionMethod.HEAD to RoutePermission.READ,
            SessionMethod.GET to RoutePermission.READ,
            SessionMethod.PUT to RoutePermission.WRITE,
            SessionMethod.POST to RoutePermission.WRITE,
            SessionMethod.DELETE to RoutePermission.WRITE
        )
    }

    fun isAuthorized() =
        isManager() || accessToken.hasPermissionTo(path, METHOD_PERMISSIONS[method]!!)

    fun isManager() =
        accessToken.hasPermission(AccessTokenPermission.MANAGER)

    fun getSessionIdentifier() =
        "${accessToken.name}@$address"

}