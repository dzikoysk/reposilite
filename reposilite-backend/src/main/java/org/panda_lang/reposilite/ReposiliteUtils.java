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

package org.panda_lang.reposilite;

import org.jetbrains.annotations.Nullable;
import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.utilities.commons.StringUtils;

public final class ReposiliteUtils {

    private ReposiliteUtils() { }

    /**
     * Process uri applying following changes:
     *
     * <ul>
     *     <li>Remove root slash</li>
     *     <li>Remove illegal path modifiers like .. and ~</li>
     * </ul>
     *
     * @param uri the uri to process
     * @return the normalized uri
     */
    public static String normalizeUri(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if (uri.contains("..") || uri.contains("~") || uri.contains(":") || uri.contains("\\")) {
            return StringUtils.EMPTY;
        }

        return uri;
    }

    public static @Nullable Repository getRepository(boolean rewritePathsEnabled, RepositoryService repositoryService, String uri) {
        String repositoryName = uri;

        if (repositoryName.startsWith("/")) {
            repositoryName = repositoryName.substring(1);
        }

        if (repositoryName.contains("..") || repositoryName.contains("~") || repositoryName.contains(":") || repositoryName.contains("\\")) {
            return null;
        }


        String repository = StringUtils.countOccurrences(repositoryName, "/") > 0
                ? repositoryName.substring(0, repositoryName.indexOf('/'))
                : repositoryName;

        if (rewritePathsEnabled && repositoryService.getRepository(repository) == null) {
            repository = repositoryService.getPrimaryRepository().getName();
        }

        return repositoryService.getRepository(repository);
    }
}
