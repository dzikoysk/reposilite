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

package com.reposilite.maven.infrastructure

import com.reposilite.maven.MavenFacade
import com.reposilite.maven.Repository
import com.reposilite.shared.ContextDsl
import com.reposilite.shared.notFoundError
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.toLocation
import com.reposilite.web.api.ReposiliteRoutes

abstract class MavenRoutes(val mavenFacade: MavenFacade) : ReposiliteRoutes() {

    fun <R> ContextDsl<R>.repository(block: (Repository?) -> Unit) {
        val repository = parameter("repository") ?: run {
            response = notFoundError("Missing repository parameter")
            return
        }

        block(mavenFacade.getRepository(repository))
    }

    fun <R> ContextDsl<R>.requireRepository(block: (Repository) -> Unit) {
        repository {
            when (it) {
                null -> response = notFoundError("Repository not found")
                else -> block(it)
            }
        }
    }

    fun <R> ContextDsl<R>.requireGav(block: (Location) -> Unit) =
        block(requireParameter("gav").toLocation())

}
