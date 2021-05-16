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

import org.panda_lang.reposilite.repository.Repository;
import org.panda_lang.reposilite.repository.RepositoryService;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Option;

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
    public static Option<String> normalizeUri(String uri) {
        if (uri.contains("..") || uri.contains("~") || uri.contains(":") || uri.contains("\\")) {
            return Option.none();
        }

        while (uri.contains("//")) {
            uri = uri.replace("//", "/");
        }
  
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        return Option.of(uri);
    }

    public static Option<Repository> getRepository(boolean rewritePathsEnabled, RepositoryService repositoryService, String uri) {
        while (uri.contains("//")) {
            uri = uri.replace("//", "/");
        }
      
        String repositoryName = uri;
      
        if (repositoryName.startsWith("/")) {
            repositoryName = repositoryName.substring(1);
        }

        if (repositoryName.contains("..") || repositoryName.contains("~") || repositoryName.contains(":") || repositoryName.contains("\\")) {
            return Option.none();
        }

        String repository = StringUtils.countOccurrences(repositoryName, "/") > 0
                ? repositoryName.substring(0, repositoryName.indexOf('/'))
                : repositoryName;

        if (rewritePathsEnabled && repositoryService.getRepository(repository) == null) {
            repository = repositoryService.getPrimaryRepository().getName();
        }

        return Option.of(repositoryService.getRepository(repository));
    }

}
