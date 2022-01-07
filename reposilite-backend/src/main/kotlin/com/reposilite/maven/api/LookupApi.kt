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

package com.reposilite.maven.api

import com.reposilite.maven.Repository
import com.reposilite.plugin.api.Event
import com.reposilite.storage.Location
import com.reposilite.token.api.AccessToken

data class LookupRequest(
    val accessToken: AccessToken?,
    val repository: String,
    val gav: Location,
) {

    fun toIdentifier(): Identifier =
        Identifier(repository, gav.toString())

}

data class VersionLookupRequest(
    val accessToken: AccessToken?,
    val repository: String,
    val gav: Location,
    val filter: String?
)

data class LatestVersionResponse(
    val isSnapshot: Boolean,
    val version: String,
)

data class VersionsResponse(
    val isSnapshot: Boolean,
    val versions: List<String>
)

class ResolveEvent(
    val accessToken: AccessToken?,
    val repository: Repository,
    val gav: Location
) : Event