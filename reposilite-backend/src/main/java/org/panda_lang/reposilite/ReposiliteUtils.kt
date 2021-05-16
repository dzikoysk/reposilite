/*
 * Copyright (c) 2020 Dzikoysk
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
package org.panda_lang.reposilite

import org.panda_lang.reposilite.maven.repository.Repository
import org.panda_lang.reposilite.maven.repository.RepositoryService
import org.panda_lang.utilities.commons.StringUtils
import org.panda_lang.utilities.commons.function.Option

object ReposiliteUtils {

    /**
     * Process uri applying following changes:
     *
     *
     *  * Remove root slash
     *  * Remove illegal path modifiers like .. and ~
     *
     *
     * @param uri the uri to process
     * @return the normalized uri
     */
    @JvmStatic
    fun normalizeUri(uri: String): Option<String> {
        var normalizedUri = uri

        if (normalizedUri.contains("..") || normalizedUri.contains("~") || normalizedUri.contains(":") || normalizedUri.contains("\\")) {
            return Option.none()
        }

        while (normalizedUri.contains("//")) {
            normalizedUri = normalizedUri.replace("//", "/")
        }

        if (normalizedUri.startsWith("/")) {
            normalizedUri = normalizedUri.substring(1)
        }

        return Option.of(normalizedUri)
    }

    @JvmStatic
    fun getRepository(rewritePathsEnabled: Boolean, repositoryService: RepositoryService, uri: String): Option<Repository> {
        var normalizedUri = uri

        while (normalizedUri.contains("//")) {
            normalizedUri = normalizedUri.replace("//", "/")
        }

        var repositoryName = normalizedUri

        if (repositoryName.startsWith("/")) {
            repositoryName = repositoryName.substring(1)
        }

        if (repositoryName.contains("..") || repositoryName.contains("~") || repositoryName.contains(":") || repositoryName.contains("\\")) {
            return Option.none()
        }

        var repository: String =
            if (StringUtils.countOccurrences(repositoryName, "/") > 0) repositoryName.substring(0, repositoryName.indexOf('/'))
            else repositoryName

        if (rewritePathsEnabled && repositoryService.getRepository(repository) == null) {
            repository = repositoryService.primaryRepository.name
        }

        return Option.of(repositoryService.getRepository(repository))
    }

}