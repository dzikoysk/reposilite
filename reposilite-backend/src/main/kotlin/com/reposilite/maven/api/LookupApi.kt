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
import com.reposilite.token.api.AccessToken
import java.nio.file.Path

data class LookupRequest(
    val accessToken: AccessToken?,
    val repository: String,
    val gav: String,
) {

    fun toIdentifier(): Identifier =
        Identifier(repository, gav)

}

data class VersionLookupRequest(
    val accessToken: AccessToken?,
    val repository: String,
    val gav: String,
    val filter: String?
)

data class VersionResponse(
    val version: String
)

data class VersionsResponse(
    val versions: List<String>
)

class ResolveEvent(
    val lookupRequest: LookupRequest,
    val repository: Repository,
    val gav: Path
) : Event