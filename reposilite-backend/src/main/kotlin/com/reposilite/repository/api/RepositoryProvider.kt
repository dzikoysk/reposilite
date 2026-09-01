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

package com.reposilite.repository.api

import com.reposilite.web.api.ReposiliteRoutes
import panda.std.reactive.Reference

interface RepositoryProvider {
    val id: String

    /**
     * Creates the protocol routes owned by this provider during HTTP server initialization.
     * Route handlers are responsible for protocol-specific
     * authentication and should use [com.reposilite.repository.RepositoryFacade] for Reposilite's
     * shared repository visibility and token rules.
     */
    fun routes(): ReposiliteRoutes

    /** Returns successfully initialized repositories and publishes their replacements after configuration reloads. */
    fun repositories(): Reference<Collection<Repository>>
}

data class ProvidedRepository(
    val provider: RepositoryProvider,
    val repository: Repository,
)
