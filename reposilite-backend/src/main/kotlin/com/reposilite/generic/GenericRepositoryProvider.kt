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

import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.web.api.ReposiliteRoutes

internal class GenericRepositoryProvider(
    private val genericFacade: GenericFacade,
    override val routes: ReposiliteRoutes,
) : RepositoryProvider {

    override val id: String = "generic"

    override fun findRepository(name: String): GenericRepository? =
        genericFacade.getRepository(name)

    override fun getRepositories(): Collection<GenericRepository> =
        genericFacade.getRepositories()
}
