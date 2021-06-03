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
package org.panda_lang.reposilite.auth

import org.panda_lang.reposilite.shared.HttpMethod
import org.panda_lang.reposilite.token.api.AccessToken
import org.panda_lang.reposilite.token.api.AccessTokenPermission.MANAGER
import org.panda_lang.reposilite.token.api.RoutePermission
import java.nio.file.Path

data class Session internal constructor(
    val path: String,
    val method: HttpMethod,
    val address: String,
    val accessToken: AccessToken,
    val availableResources: List<Path>
) {

    fun isAuthorized() =
        isManager() || accessToken.hasPermissionTo(path, RoutePermission.findByMethod(method)!!)

    fun isManager() =
        accessToken.hasPermission(MANAGER)

    fun getSessionIdentifier() =
        "${accessToken.alias}@$address"

}