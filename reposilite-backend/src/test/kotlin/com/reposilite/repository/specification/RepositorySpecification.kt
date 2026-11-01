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

package com.reposilite.repository.specification

import com.reposilite.repository.RepositoryAccessResolver
import com.reposilite.repository.RepositoryFacade
import com.reposilite.repository.api.RepositoryInfo
import com.reposilite.repository.api.RepositoryProvider
import com.reposilite.repository.api.RepositoryVisibility.PUBLIC
import com.reposilite.token.specification.AccessTokenSpecification
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.api.ReposiliteRoutes

internal abstract class RepositorySpecification : AccessTokenSpecification() {

    protected val repositoryFacade = RepositoryFacade(RepositoryAccessResolver(accessTokenFacade))

    protected fun useRoutes(vararg endpoints: ReposiliteRoute<*>): ReposiliteRoutes =
        object : ReposiliteRoutes() {
            override val routes = endpoints.toSet()
        }

    protected fun useRepository(name: String): RepositoryInfo =
        object : RepositoryInfo {
            override val name = name
            override val visibility = PUBLIC
        }

    protected fun useRepositoryProvider(vararg names: String): TestRepositoryProvider =
        TestRepositoryProvider(names.map { useRepository(it) })

    protected class TestRepositoryProvider(var currentRepositories: Collection<RepositoryInfo>) : RepositoryProvider {
        override fun getRepositories(): Collection<RepositoryInfo> =
            currentRepositories
    }

}
