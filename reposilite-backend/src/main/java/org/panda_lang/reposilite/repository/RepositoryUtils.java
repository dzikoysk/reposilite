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

package org.panda_lang.reposilite.repository;

import org.panda_lang.reposilite.config.Configuration;
import org.panda_lang.utilities.commons.StringUtils;

import java.util.regex.Pattern;

public final class RepositoryUtils {

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("\\W+/");

    private RepositoryUtils() { }

    /**
     * Process uri applying following changes:
     *
     * <ul>
     *     <li>Remove root slash</li>
     *     <li>Remove illegal path modifiers like .. and ~</li>
     *     <li>Insert repository name if missing</li>
     * </ul>
     *
     * @param configuration the configuration with repositories list
     * @param uri the uri to process
     * @return the normalized uri
     */
    public static String normalizeUri(Configuration configuration, RepositoryService repositoryService, String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if (uri.contains("..") || uri.contains("~") || uri.contains(":") || uri.contains("\\")) {
            return StringUtils.EMPTY;
        }

        if (!configuration.rewritePathsEnabled) {
            return uri;
        }

        if (StringUtils.countOccurrences(uri, "/") <= 1) {
            return uri;
        }

        for (Repository repository : repositoryService.getRepositories()) {
            if (uri.startsWith(repository.getName())) {
                return uri;
            }
        }

        return repositoryService.getPrimaryRepository().getName() + "/" + uri;
    }

}
